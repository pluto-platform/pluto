import charon.Charon.Combine
import charon.Tilelink
import chisel3._
import charon.Charon.RangeBinder
import lib.util.{BundleItemAssignment, Exponential, SeqToTransposable, SeqToVecMethods}
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryOperation}
import cores.nix.Nix
import peripherals.uart.{Uart, UartReceiver, UartTransmitter}
import peripherals.{Leds, ProgramMemory}

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
  val uart = Module(new Uart(115200, 100000000))
  val led = Module(new Leds(1))

  io.led := led.io.leds(0)
  io.tx := uart.io.tx
  uart.io.rx := io.rx

  core.io.instructionRequester <> prog.io.tilelink


  core.io.dataRequester <=> Combine(Seq(
    led.io.tilelink.bind(0x10000),
    uart.io.tilelinkInterface.bind(0x20000)
  ))

  /*
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

  when(core.io.dataRequester.a.valid && core.io.dataRequester.a.address === 0x10000.U) {
    ledReg := !ledReg
  }
  */


}

object TopCachedEmitter extends App {
  emitVerilog(new Top, Array("--target-dir","build"))
}
