package peripherals.uart

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.{Valid, is, switch}
import peripherals.uart.UartReceiver.State

object UART {
  object Baud {
    9600
  }
}

class UART extends Module {

  val io = IO(new Bundle {
    val rx = Input(Bool())
    val tx = Output(Bool())
  })


}



