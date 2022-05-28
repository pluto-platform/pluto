
import chisel3._
import lib.Binaries
import cores.nix.Nix
import charon.Charon.{Combine, Link, RangeBinder}
import peripherals.{BlockRam, Button, Leds, ProgramMemory, Sram}
import peripherals.uart.Uart

class TopCached extends Module {
  val io = IO(new Bundle {
    val led = Output(UInt(8.W))
    val rx = Input(Bool())
    val tx = Output(Bool())
    val button = Input(Bool())
  })

  val prog = ProgramMemory(Binaries.loadBytes("../pluto-rt/rust.bin"))
  val uart = Uart(115200, 50000000)(io.rx, io.tx)
  val led = Leds(io.led)
  val ram = BlockRam(1024)
  val button = Button(io.button)

  val core = Nix(
    _.externalInterrupt := button.io.interrupt,
    _.customInterrupts(0) := uart.io.interrupt
  )

  Link(
    core.io.instructionRequester,
    core.io.dataRequester
  )(
    prog.io.tilelink.bind(0x0),
    uart.io.tilelinkInterface.bind(0x20000),
    Combine(
      led.io.tilelink.bind(0x10000),
      button.io.tilelink.bind(0x30000)
    ),
    ram.io.tilelink.bind(0x80000000L)
  )
}

object TopCachedEmitter extends App {
  emitVerilog(new TopCached, Array("--target-dir","build"))
}
