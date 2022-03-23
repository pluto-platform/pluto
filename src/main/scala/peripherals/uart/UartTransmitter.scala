package peripherals.uart

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import peripherals.uart.UartTransmitter.State

object UartTransmitter {
  object State extends ChiselEnum {
    val Idle, Send, Stop = Value
  }
}
class UartTransmitter(val pv: Int) extends Module {

  val io = IO(new Bundle {

    val tx = Output(Bool())
    val send = Flipped(Decoupled(UInt(8.W)))

  })

  val countReg = RegInit(0.U(32.W))
  val periodReg = RegInit(pv.U)
  val tickReg = RegInit(0.U(4.W))
  val stateReg = RegInit(State.Idle)

  val periodTick = periodReg === countReg

  val shiftReg = RegInit(0.U(9.W))

  io.tx := 1.B
  io.send.ready := 0.B

  countReg := countReg + 1.U

  switch(stateReg) {
    is(State.Idle) {
      io.send.ready := 1.B
      countReg := 0.U
      tickReg := 0.U
      shiftReg := io.send.bits ## 0.B
      stateReg := Mux(io.send.valid, State.Send, State.Idle)
    }
    is(State.Send) {
      io.tx := shiftReg(0)
      when(tickReg > 8.U) {
        stateReg := State.Stop
        io.tx := 1.B
      }.elsewhen(periodTick) {
        shiftReg := 0.B ## shiftReg(8,1)
        countReg := 0.U
        tickReg := tickReg + 1.U
        stateReg := State.Send
      }.otherwise {
        stateReg := State.Send
      }
    }
    is(State.Stop) {
      stateReg := Mux(periodTick, State.Idle, State.Stop)
    }
  }

}
