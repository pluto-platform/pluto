package peripherals.uart

import chisel3._
import chisel3.util.{Queue, is, switch}
import charon.Tilelink
import charon.Tilelink.{Agent, Operation}
import lib.util.{ByteSplitter, Delay, SeqConcat}
object Uart {
}

class Uart(initialBaud: Int, frequency: Int) extends Module {

  val io = IO(new Bundle {
    val rx = Input(Bool())
    val tx = Output(Bool())
    val clientPort = Agent.Interface.Responder(Tilelink.Parameters(w = 2, a = 2, z = 2))
  })

  val transmitter = Module(new UartTransmitter)
  val receiver = Module(new UartReceiver)
  val receiverQueue = Queue(receiver.io.received, 4)

  val periodReg = RegInit(UInt(20.W), (frequency/initialBaud).U)


  switch(Delay(io.clientPort.a.address)) {
    is(0x00.U) {
      io.clientPort.d.data(0) := transmitter.io.send.ready ## receiverQueue.valid
    }
    is(0x01.U) {
      io.clientPort.d.data := periodReg.toBytes(4)
      when(io.clientPort.a.opcode.isOneOf(Operation.PutFullData, Operation.PutPartialData)) {
        periodReg := io.clientPort.a.data.concat.apply(19,0)
      }
    }
    is(0x02.U) {
      io.clientPort.d.data := receiverQueue.bits.toBytes(4)
      receiverQueue.ready := io.clientPort.a.opcode === Operation.Get

      when(io.clientPort.a.opcode.isOneOf(Operation.PutFullData, Operation.PutPartialData)) {
        transmitter.io.send.enq(io.clientPort.a.data(0))
      }
    }
  }




}



