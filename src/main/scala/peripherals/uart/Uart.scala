package peripherals.uart

import Chisel.Cat
import chisel3._
import chisel3.util.{DecoupledIO, MuxLookup, Queue, RegEnable, is, switch}
import charon.Tilelink
import charon.Tilelink.{Agent, Operation, Response}
import lib.util.{BundleItemAssignment, ByteSplitter, Delay, OneOf, SeqConcat, SeqToVecMethods}


object Uart {
  def apply(initialBaud: Int, frequency: Int)(rx: Bool, tx: Bool): Uart = {
    val mod = Module(new Uart(initialBaud, frequency))
    mod.io.rx := rx
    tx := mod.io.tx
    mod
  }
}

class Uart(initialBaud: Int, frequency: Int) extends Module {

  val io = IO(new Bundle {
    val rx = Input(Bool())
    val tx = Output(Bool())
    val interrupt = Output(Bool())
    val tilelinkInterface = Agent.Interface.Responder(Tilelink.Parameters(w = 4, a = 4, z = 2, Some(10), Some(10)))
  })

  val transmitter = Module(new UartTransmitter)
  val receiver = Module(new Rx(50000000, 115200))

  io.tx := transmitter.io.tx
  receiver.io.rxd := io.rx

  transmitter.io.send.valid := 0.B
  transmitter.io.send.bits := 0.U


  val receiverQueue = Queue(receiver.io.channel, 4)
  receiverQueue.ready := 0.B
  io.interrupt := receiverQueue.valid

  val periodReg = RegInit(UInt(20.W), (frequency/initialBaud).U)
  transmitter.io.period := periodReg
  //receiver.io.period := periodReg

  val responseReg = RegEnable(Mux(io.tilelinkInterface.a.bits.opcode === Operation.Get, Response.AccessAckData, Response.AccessAck), io.tilelinkInterface.d.ready)
  val responseValidPipe = RegEnable(io.tilelinkInterface.a.valid, 0.B, io.tilelinkInterface.d.ready)
  val deniedReg = RegEnable(io.tilelinkInterface.a.bits.address.isOneOf(0x00.U, 0x04.U, 0x08.U), io.tilelinkInterface.d.ready)

  io.tilelinkInterface.a.ready := io.tilelinkInterface.d.ready || (!io.tilelinkInterface.d.ready && !responseValidPipe)

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
      receiverQueue.ready := RegNext(io.tilelinkInterface.a.bits.opcode === Operation.Get && io.tilelinkInterface.a.valid, 0.B)
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


class UartIO extends DecoupledIO(UInt(8.W)) {
}

class Tx(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val txd = Output(UInt(1.W))
    val channel = Flipped(new UartIO())
  })

  val BIT_CNT = ((frequency + baudRate / 2) / baudRate - 1).asUInt

  val shiftReg = RegInit(0x7ff.U)
  val cntReg = RegInit(0.U(20.W))
  val bitsReg = RegInit(0.U(4.W))

  io.channel.ready := (cntReg === 0.U) && (bitsReg === 0.U)
  io.txd := shiftReg(0)

  when(cntReg === 0.U) {

    cntReg := BIT_CNT
    when(bitsReg =/= 0.U) {
      val shift = shiftReg >> 1
      shiftReg := Cat(1.U, shift(9, 0))
      bitsReg := bitsReg - 1.U
    }.otherwise {
      when(io.channel.valid) {
        shiftReg := Cat(Cat(3.U, io.channel.bits), 0.U) // two stop bits, data, one start bit
        bitsReg := 11.U
      }.otherwise {
        shiftReg := 0x7ff.U
      }
    }

  }.otherwise {
    cntReg := cntReg - 1.U
  }
}

/**
 * Receive part of the UART.
 * A minimal version without any additional buffering.
 * Use a ready/valid handshaking.
 *
 * The following code is inspired by Tommy's receive code at:
 * https://github.com/tommythorn/yarvi
 */
class Rx(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val rxd = Input(UInt(1.W))
    val channel = new UartIO()
  })

  val BIT_CNT = ((frequency + baudRate / 2) / baudRate - 1).U
  val START_CNT = ((3 * frequency / 2 + baudRate / 2) / baudRate - 1).U

  // Sync in the asynchronous RX data, reset to 1 to not start reading after a reset
  val rxReg = RegNext(RegNext(io.rxd, 1.U), 1.U)

  val shiftReg = RegInit(0.U(8.W))
  val cntReg = RegInit(0.U(20.W))
  val bitsReg = RegInit(0.U(4.W))
  val valReg = RegInit(false.B)

  when(cntReg =/= 0.U) {
    cntReg := cntReg - 1.U
  }.elsewhen(bitsReg =/= 0.U) {
    cntReg := BIT_CNT
    shiftReg := Cat(rxReg, shiftReg >> 1)
    bitsReg := bitsReg - 1.U
    // the last shifted in
    when(bitsReg === 1.U) {
      valReg := true.B
    }
  }.elsewhen(rxReg === 0.U) { // wait 1.5 bits after falling edge of start
    cntReg := START_CNT
    bitsReg := 8.U
  }

  when(valReg && io.channel.ready) {
    valReg := false.B
  }

  io.channel.bits := shiftReg
  io.channel.valid := valReg
}

