package peripherals.uart

import chisel3.experimental.ChiselEnum
import chisel3.util.{is, switch}
import chisel3._
import peripherals.uart.UartReceiver.State

object UartReceiver {
  object State extends ChiselEnum {
    val Idle, Positioning, Receiving = Value
  }
}
class UartReceiver(val pv: Int) extends Module {

  val io = IO(new Bundle {

    val rx = Input(Bool())
    val received = Output(UInt(11.W))
    val valid = Output(Bool())

  })


  val counterReg = RegInit(0.U(32.W))
  val periodReg = RegInit(pv.U)
  val tickReg = RegInit(0.U(4.W))
  val stateReg = RegInit(State.Idle)

  val halfPeriodTick = (periodReg >> 1).asUInt === counterReg
  val periodTick = periodReg === counterReg
  val rxFallingEdge = RegNext(RegNext(io.rx)) && ! io.rx

  val shiftReg = RegInit(0.U(8.W))
  io.received := shiftReg
  io.valid := 0.B

  counterReg := Mux(stateReg.isOneOf(State.Positioning, State.Receiving), counterReg + 1.U, counterReg)

  switch(stateReg) {
    is(State.Idle) {
      io.valid := 1.B
      stateReg := Mux(rxFallingEdge, State.Positioning, State.Idle)

    }
    is(State.Positioning) {

      when(io.rx) { // abort when line goes high again
        stateReg := State.Idle
      }.elsewhen(halfPeriodTick) {
        stateReg := State.Receiving
        counterReg := 0.U
        tickReg := 0.U
      } otherwise {
        stateReg := State.Positioning
      }

    }
    is(State.Receiving) {

      when(tickReg > 7.U) {
        stateReg := State.Idle
      }.elsewhen(periodTick) {
        shiftReg := io.rx ## shiftReg(7,1)
        counterReg := 0.U
        tickReg := tickReg + 1.U
        stateReg := State.Receiving
      }.otherwise {
        stateReg := State.Receiving
      }

    }
  }


}