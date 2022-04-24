package peripherals.uart

import chisel3._
import chisel3.util.{MuxLookup, Queue, RegEnable, is, switch}
import charon.Tilelink
import charon.Tilelink.{Agent, Operation, Response}
import lib.util.{BundleItemAssignment, ByteSplitter, Delay, OneOf, SeqConcat, SeqToVecMethods}
object Uart {
}

class Uart(initialBaud: Int, frequency: Int) extends Module {

  val io = IO(new Bundle {
    val rx = Input(Bool())
    val tx = Output(Bool())
    val tilelinkInterface = Agent.Interface.Responder(Tilelink.Parameters(w = 4, a = 4, z = 2, Some(10), Some(10)))
  })

  val transmitter = Module(new UartTransmitter)
  val receiver = Module(new UartReceiver)

  io.tx := transmitter.io.tx
  receiver.io.rx := io.rx

  transmitter.io.send.valid := 0.B
  transmitter.io.send.bits := 0.U


  val receiverQueue = Queue(receiver.io.received, 4)
  receiverQueue.ready := 0.B

  val periodReg = RegInit(UInt(20.W), (frequency/initialBaud).U)
  transmitter.io.period := periodReg
  receiver.io.period := periodReg

  val responseReg = RegEnable(Mux(io.tilelinkInterface.a.bits.opcode === Operation.Get, Response.AccessAckData, Response.AccessAck), io.tilelinkInterface.d.ready)
  val responseValidPipe = RegEnable(io.tilelinkInterface.a.valid, 0.B, io.tilelinkInterface.d.ready)
  val deniedReg = RegEnable(io.tilelinkInterface.a.bits.address.isOneOf(0x00.U, 0x04.U, 0x08.U), io.tilelinkInterface.d.ready)

  io.tilelinkInterface.a.ready := io.tilelinkInterface.d.ready || !responseValidPipe

  io.tilelinkInterface.d.bits.set(
    _.opcode := responseReg,
    _.param := 0.U,
    _.size := 2.U,
    _.source := RegEnable(io.tilelinkInterface.a.bits.source, io.tilelinkInterface.d.ready),
    _.sink := 0.U,
    _.denied := deniedReg,
    _.data := 0.U.toBytes(4),
    _.corrupt := 0.U
  )
  io.tilelinkInterface.d.valid := responseValidPipe

  switch(RegEnable(io.tilelinkInterface.a.bits.address, 0.U, io.tilelinkInterface.d.ready)) {
    is(0x00.U) {
      io.tilelinkInterface.d.bits.data(0) := transmitter.io.send.ready ## receiverQueue.valid
    }
    is(0x04.U) {
      io.tilelinkInterface.d.bits.data := periodReg.toBytes(4)
    }
    is(0x08.U) {
      io.tilelinkInterface.d.bits.data := receiverQueue.bits.toBytes(4)
      receiverQueue.ready := io.tilelinkInterface.a.bits.opcode === Operation.Get
    }
  }
  when(io.tilelinkInterface.a.bits.opcode.isOneOf(Operation.PutFullData, Operation.PutPartialData) && io.tilelinkInterface.a.valid) {
    switch(io.tilelinkInterface.a.bits.address) {
      is(0x04.U) {
        periodReg := io.tilelinkInterface.a.bits.data.concat.apply(19,0)
      }
      is(0x08.U) {
        transmitter.io.send.enq(io.tilelinkInterface.a.bits.data(0))
      }
    }
  }





}



