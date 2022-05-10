import charon.Charon.{Combine, Link, RangeBinder}
import charon.Tilelink
import chisel3._
import lib.util.{BundleItemAssignment, Exponential, SeqToTransposable, SeqToVecMethods}
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryOperation}
import cores.nix.Nix
import peripherals.uart.{Uart, UartReceiver, UartTransmitter}
import peripherals.{BlockRam, Leds, ProgramMemory}

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

  val core = Module(new Nix)
  core.io.interrupts := Seq.fill(16)(0.B).toVec

  io.pc := core.io.instructionRequester.a.bits.address

  val prog = Module(new ProgramMemory(program))
  val uart = Module(new Uart(115200, 50000000))
  val led = Module(new Leds(1))
  val ram = Module(new BlockRam(1024))

  io.led := led.io.leds(0)
  io.tx := uart.io.tx
  uart.io.rx := io.rx

  Link(
    Seq(core.io.instructionRequester, core.io.dataRequester),
    Seq(
      prog.io.tilelink.bind(0x0),
      led.io.tilelink.bind(0x10000),
      uart.io.tilelinkInterface.bind(0x20000),
      ram.io.tilelink.bind(0x80000000L)
    )
  )



}

object TopCachedEmitter extends App {
  emitVerilog(new TopCached, Array("--target-dir","build"))
}
