package core.pipeline

import chisel3._
import core.Branching
import core.Pipeline.InstructionChannel
import lib.util.BundleItemAssignment

object ProgramCounter {

}

class ProgramCounter(init: Option[BigInt] = None) extends Module{

  val io = IO(new Bundle {
    val instructionRequest = new InstructionChannel.Request
    val value = Output(UInt(32.W))
    val stall = Input(Bool())
    val branching = new Branching.ProgramCounterChannel
  })

  val reg = RegInit(init.getOrElse(BigInt(0)).U(32.W))

  val nextReg = Mux(io.stall, reg, io.branching.next)
  reg := nextReg

  io.instructionRequest.set(
    _.valid := 1.B,
    _.bits.address := nextReg
  )
  io.value := reg
}
