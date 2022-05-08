package cores.nix

import chisel3._
import cores.lib.Exception
import cores.lib.Exception.ExceptionBundle
import cores.nix.ExceptionUnit.WriteBackChannel
import lib.util.{BoolVec, BundleItemAssignment}

object ExceptionUnit {
  class ProgramCounterChannel extends Bundle {
    val target = Input(UInt(32.W))
    val jump = Input(Bool())
  }
  class WriteBackChannel extends Bundle {
    val exception = Output(new ExceptionBundle)
    val mret = Output(Bool())
    val isBubble = Output(Bool())
    val flush = Input(Bool())
  }
}

class ExceptionUnit extends Module {

  val io = IO(new Bundle {

    val writeBack = Flipped(new WriteBackChannel)
    val csr = Flipped(new Exception.CSRChannel)
    val programCounter = Flipped(new ExceptionUnit.ProgramCounterChannel)

  })

  val customInterrupts = io.csr.interruptPending.custom
    .zip(io.csr.interruptEnable.custom)
    .map { case (pending, enable) =>
      pending && enable
    }

  io.programCounter.target := io.csr.mtvec
  io.programCounter.jump := 0.B
  io.writeBack.flush := 0.B

  val restartPc = io.writeBack.exception.pc + 4.U

  io.csr.newException.set(
    _.exception := 0.B,
    _.value := 0.U,
    _.cause := Exception.Cause.None,
    _.pc := DontCare
  )
  io.csr.set(
    _.resetMie := 0.B,
    _.restoreMie := 0.B,
    _.updateMpie := 0.B
  )

  when(io.csr.interruptEnable.global) {
    when(customInterrupts.orR && !io.writeBack.isBubble) {
      io.writeBack.flush := 1.B
      io.programCounter.jump := 1.B
      io.csr.updateMpie := 1.B
      io.csr.resetMie := 1.B
      io.csr.newException.set(
        _.exception := 1.B,
        _.value := 0.U,
        _.pc := restartPc,
        _.cause := Exception.Cause.MachineExternalInterrupt
      )
    }.elsewhen(io.csr.interruptPending.external && io.csr.interruptEnable.external && !io.writeBack.isBubble) {
      io.programCounter.jump := 1.B
      io.writeBack.flush := 1.B
      io.csr.updateMpie := 1.B
      io.csr.resetMie := 1.B
      io.csr.newException.set(
        _.exception := 1.B,
        _.value := 0.U,
        _.pc := restartPc,
        _.cause := Exception.Cause.MachineExternalInterrupt
      )
    }.elsewhen(io.csr.interruptPending.timer && io.csr.interruptEnable.timer && !io.writeBack.isBubble) {
      io.programCounter.jump := 1.B
      io.writeBack.flush := 1.B
      io.csr.updateMpie := 1.B
      io.csr.resetMie := 1.B
      io.csr.newException.set(
        _.exception := 1.B,
        _.value := 0.U,
        _.pc := restartPc,
        _.cause := Exception.Cause.MachineTimerInterrupt
      )
    }.elsewhen(io.csr.interruptPending.software && io.csr.interruptEnable.software && !io.writeBack.isBubble) {
      io.programCounter.jump := 1.B
      io.writeBack.flush := 1.B
      io.csr.updateMpie := 1.B
      io.csr.resetMie := 1.B
      io.csr.newException.set(
        _.exception := 1.B,
        _.value := 0.U,
        _.pc := restartPc,
        _.cause := Exception.Cause.MachineSoftwareInterrupt
      )
    }.elsewhen(io.writeBack.exception.exception) {
      io.writeBack.flush := 1.B
      io.programCounter.jump := 1.B
      io.csr.updateMpie := 1.B
      io.csr.resetMie := 1.B
      io.csr.newException := io.writeBack.exception
      when(io.writeBack.exception.cause === Exception.Cause.EnvironmentCallFromMachineMode) {
        io.csr.newException.pc := restartPc
      }
    }.elsewhen(io.writeBack.mret) {
      io.writeBack.flush := 1.B
      io.programCounter.jump := 1.B
      io.csr.restoreMie := 1.B
      io.programCounter.target := io.csr.mepc
    }

  }.elsewhen(io.writeBack.exception.exception) {
    io.writeBack.flush := 1.B
    io.csr.updateMpie := 1.B
    io.csr.resetMie := 1.B
    io.programCounter.jump := 1.B
    io.csr.newException := io.writeBack.exception
    when(io.writeBack.exception.cause === Exception.Cause.EnvironmentCallFromMachineMode) {
      io.csr.newException.pc := restartPc
    }
  }.elsewhen(io.writeBack.mret) {
    io.writeBack.flush := 1.B
    io.programCounter.jump := 1.B
    io.csr.restoreMie := 1.B
    io.programCounter.target := io.csr.mepc
  }



}
