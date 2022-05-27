import charon.Charon.{Combine, Link, RangeBinder}
import charon.Tilelink
import chisel3._
import chisel3.experimental.attach
import lib.util.{BundleItemAssignment, Delay, Exponential, SeqToTransposable, SeqToVecMethods}
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryOperation}
import cores.nix.Nix
import peripherals.uart.{Uart, UartReceiver, UartTransmitter}
import peripherals.{BlockRam, Button, Leds, ProgramMemory, SevenSegmentDisplay, Sram}

import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}

class TopCached extends Module {
  val io = IO(new Bundle {
    val led = Output(Bool())
    val pc = Output(UInt(10.W))
    val rx = Input(Bool())
    val cts = Output(Bool())
    val tx = Output(Bool())
    val button = Input(Bool())
    val sram = new Sram.IO
  })

  io.cts := 1.B



  val programBinary = Files.readAllBytes(Paths.get("../pluto-rt/rust.bin"))
    .map(_.toLong & 0xFF)
    .map(BigInt(_)) ++ Seq.fill(16)(BigInt(0))
  val program = programBinary
    .grouped(4)
    .map(a => a(0) | (a(1) << 8) | (a(2) << 16) | (a(3) << 24))
    .toSeq

  val core = Module(new Nix)
  core.io.customInterrupts := Seq.fill(16)(0.B).toVec
  core.io.timerInterrupt := 0.B


  val prog = Module(new ProgramMemory(program))
  val uart = Module(new Uart(115200, 50000000))
  val led = Module(new Leds(1))
  val ram = Module(new BlockRam(1024))
  val button = Module(new Button)
  val sram = Module(new Sram)
  sram.io.sram <> io.sram
  attach(sram.io.sram.data, io.sram.data)

  core.io.externalInterrupt := button.io.interrupt
  core.io.customInterrupts(0) := uart.io.interrupt
  button.io.button := io.button

  io.pc := core.io.instructionRequester.a.valid ## prog.io.tilelink.d.valid ## core.io.dataRequester.a.valid ## ram.io.tilelink.d.valid

  io.led := led.io.leds(0)
  io.tx := uart.io.tx
  uart.io.rx := io.rx

  Link(
    Seq(core.io.instructionRequester, core.io.dataRequester),
    Seq(
      prog.io.tilelink.bind(0x0),
      uart.io.tilelinkInterface.bind(0x20000),
      Combine(Seq(
        led.io.tilelink.bind(0x10000),
        button.io.tilelink.bind(0x30000)
      )),
      ram.io.tilelink.bind(0x80000000L),
      sram.io.tilelink.bind(0x90000000L)
    )
  )











}

object TopCachedEmitter extends App {
  emitVerilog(new TopCached, Array("--target-dir","build"))
}
