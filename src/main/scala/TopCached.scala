import charon.Tilelink
import chisel3._
import chisel3.experimental.{ChiselAnnotation, annotate}
import chisel3.util.experimental.loadMemoryFromFileInline
import firrtl.annotations.MemorySynthInit
import lib.util.{BundleItemAssignment, SeqToTransposable, SeqToVecMethods}
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryOperation}
import cores.nix.Nix
import peripherals.uart.{UartReceiver, UartTransmitter}
import peripherals.ProgramMemory

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}

class TopCached extends Module {
  val io = IO(new Bundle {
    val led = Output(Bool())
    val pc = Output(UInt(10.W))
    val rx = Input(Bool())
    val tx = Output(Bool())
  })

  val programBinary = Files.readAllBytes(Paths.get("asm/blinkTest.bin"))
    .map(_.toLong & 0xFF)
    .map(BigInt(_)) ++ Seq.fill(16)(BigInt(0))
  val program = programBinary
    .grouped(4)
    .map(a => a(0) | (a(1) << 8) | (a(2) << 16) | (a(3) << 24))
    .toSeq

  io.tx := 0.B
  io.pc := 0.U

  val core = Module(new Nix)
  core.io.interrupts := Seq.fill(16)(0.B).toVec

  val prog = Module(new ProgramMemory(program))

  core.io.instructionRequester <> prog.io.tilelink

  core.io.dataRequester.a.ready := 1.B

  core.io.dataRequester.d.set(
    _.opcode := Tilelink.Response.AccessAck,
    _.param := 0.U,
    _.size := 2.U,
    _.source := 0.U,
    _.sink := 0.U,
    _.denied := 0.B,
    _.data := Seq.fill(4)(0.U).toVec,
    _.corrupt := 0.B,
    _.valid := RegNext(core.io.dataRequester.a.valid, 0.B)
  )

  val ledReg = RegInit(0.B)
  io.led := ledReg

  when(core.io.dataRequester.a.valid) {
    ledReg := !ledReg
  }

}

object TopCachedEmitter extends App {
  emitVerilog(new Top, Array("--target-dir","build"))
}
