package peripherals.uart

import chisel3._
import chisel3.util.{MuxLookup, Queue, RegEnable, is, switch}
import charon.Tilelink
import charon.Tilelink.{Agent, Operation, Response}
import lib.util.{ByteSplitter, Delay, OneOf, SeqConcat, SeqToVecMethods}
object Uart {
}

class Uart(initialBaud: Int, frequency: Int) extends Module {

  val io = IO(new Bundle {
    val rx = Input(Bool())
    val tx = Output(Bool())
    val tilelinkInterface = Agent.Interface.Responder(Tilelink.Parameters(w = 2, a = 2, z = 2))
  })

  val transmitter = Module(new UartTransmitter)
  val receiver = Module(new UartReceiver)
  val receiverQueue = Queue(receiver.io.received, 4)

  val periodReg = RegInit(UInt(20.W), (frequency/initialBaud).U)

  val responseReg = RegEnable(Mux(io.tilelinkInterface.a.opcode === Operation.Get, Response.AccessAckData, Response.AccessAck), io.tilelinkInterface.d.ready)
  val responseValidPipe = RegEnable(io.tilelinkInterface.a.valid, 0.B, io.tilelinkInterface.d.ready)
  val deniedReg = RegEnable(io.tilelinkInterface.a.address.isOneOf(0x00.U, 0x04.U, 0x08.U), io.tilelinkInterface.d.ready)

  io.tilelinkInterface.a.ready := io.tilelinkInterface.d.ready

  switch(RegEnable(io.tilelinkInterface.a.address, 0.U, io.tilelinkInterface.d.ready)) {
    is(0x00.U) {
      io.tilelinkInterface.d.data(0) := transmitter.io.send.ready ## receiverQueue.valid
    }
    is(0x04.U) {
      io.tilelinkInterface.d.data := periodReg.toBytes(4)
    }
    is(0x08.U) {
      io.tilelinkInterface.d.data := receiverQueue.bits.toBytes(4)
      receiverQueue.ready := io.tilelinkInterface.a.opcode === Operation.Get
    }
  }
  when(io.tilelinkInterface.a.opcode.isOneOf(Operation.PutFullData, Operation.PutPartialData)) {
    switch(io.tilelinkInterface.a.address) {
      is(0x04.U) {
        periodReg := io.tilelinkInterface.a.data.concat.apply(19,0)
      }
      is(0x08.U) {
        transmitter.io.send.enq(io.tilelinkInterface.a.data(0))
      }
    }
  }





}



