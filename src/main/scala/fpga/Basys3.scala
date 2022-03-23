package fpga

import chisel3._
import peripherals.uart.{UartReceiver, UartTransmitter}

object Basys3 {
  class Basys3IO extends Bundle {

    val switches = Input(UInt(16.W))
    val leds = Output(UInt(16.W))
    val uart = new Bundle {
      val rx = Input(Bool())
      val tx = Output(Bool())
    }

  }
}

class Basys3 extends Module {

  val io = IO(new Basys3.Basys3IO)

  val receiver = Module(new UartReceiver(10417))
  val transmitter = Module(new UartTransmitter(10417))

  receiver.io.rx := io.uart.rx
  io.leds := receiver.io.received
  io.uart.tx := transmitter.io.tx

  transmitter.io.send.valid := transmitter.io.send.ready && RegNext(!receiver.io.valid) && receiver.io.valid
  transmitter.io.send.bits := receiver.io.received

}
object Basys3Emitter extends App {
  emitVerilog(new Basys3)
}
