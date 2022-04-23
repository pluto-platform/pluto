package peripherals.uart

import chisel3.experimental.ChiselEnum
import chisel3.util.{Decoupled, is, switch}
import chisel3._
import peripherals.uart.UartReceiver.State
import lib.Types.Byte
import lib.util.{Delay, synchronize}
object UartReceiver {
  object State extends ChiselEnum {
    val Idle, Positioning, Receiving, Enqueue = Value
  }
}
class UartReceiver extends Module {

  val io = IO(new Bundle {
    val rx = Input(Bool())
    val period = Input(UInt(32.W))
    val received = Decoupled(Byte())
  })


  val counterReg = RegInit(0.U(20.W))
  val tickReg = RegInit(0.U(4.W))
  val stateReg = RegInit(State.Idle)

  val halfPeriodTick = counterReg === (io.period >> 1).asUInt
  val periodTick = counterReg === io.period

  val synchronizedRx = synchronize(io.rx)
  val rxFallingEdge = Delay(synchronizedRx) && !synchronizedRx

  val shiftReg = RegInit(0.U(8.W))
  io.received.bits := shiftReg
  io.received.valid := 0.B

  counterReg := Mux(
    stateReg.isOneOf(State.Positioning, State.Receiving),
    counterReg + 1.U,
    counterReg
  )

  switch(stateReg) {
    is(State.Idle) {
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
    is(State.Enqueue) {
      io.received.valid := 1.B
      stateReg := Mux(io.received.ready, State.Idle, State.Enqueue)
    }
  }


}