package peripherals

import charon.Tilelink
import chisel3._
import chisel3.experimental.{Analog, ChiselEnum, attach}
import chisel3.util.{RegEnable, is, switch}
import lib.modules.TriStateDriver
import lib.util.{BundleItemAssignment, ByteSplitter}
import peripherals.Sram.State

object Sram {

  class IO extends Bundle {
    val address = Output(UInt(20.W))
    val data = Analog(16.W)
    val outputEnable = Output(Bool())
    val writeEnable = Output(Bool())
    val chipSelect = Output(Bool())
    val strobe = Output(Vec(2,Bool()))
  }

  def apply(interface: IO): Sram = {
    val mod = Module(new Sram)
    mod.io.sram <> interface
    attach(mod.io.sram.data, interface.data)
    mod
  }

  object State extends ChiselEnum {
    val Idle, Receive, Respond = Value
  }

}

class Sram extends Module {

  val io = IO(new Bundle {
    val tilelink = Tilelink.Agent.Interface.Responder(Tilelink.Parameters(4, 11, 2, Some(10), Some(10)))
    val sram = new Sram.IO
  })


  val (readBus, writeBus) = TriStateDriver(io.sram.data)

  writeBus.set(
    _.valid := 0.B,
    _.bits := DontCare
  )

  val stateReg = RegInit(State.Idle)

  io.sram.set(
    _.strobe := VecInit(1.B, 1.B),
    _.outputEnable := 1.B,
    _.chipSelect := 0.B,
    _.writeEnable := 0.B,
    _.address := DontCare
  )

  val reqPipe = RegEnable(io.tilelink.a.bits, 0.U.asTypeOf(io.tilelink.a.bits), stateReg === State.Idle)

  val dataReg = RegInit(0.U(16.W))

  io.tilelink.d.valid := 0.B
  io.tilelink.a.ready := 0.B

  io.tilelink.d.bits.set(
    _.opcode := Mux(reqPipe.opcode === Tilelink.Operation.Get, Tilelink.Response.AccessAckData, Tilelink.Response.AccessAck),
    _.param := 0.U,
    _.size := 0.U,
    _.source := reqPipe.source,
    _.sink := 0.U,
    _.denied := reqPipe.address(1,0) =/= "b00".U || reqPipe.size =/= 2.U,
    _.data := (readBus ## dataReg).toBytes(4),
    _.corrupt := 0.B
  )

  switch(stateReg) {
    is(State.Idle) {

      io.tilelink.a.ready := 1.B

      io.sram.address := io.tilelink.a.bits.address(10,2) ## 0.B
      writeBus.bits := io.tilelink.a.bits.data(1) ## io.tilelink.a.bits.data(0)



      when(io.tilelink.a.valid) {
        stateReg := State.Receive

        val write = io.tilelink.a.bits.opcode.isOneOf(Tilelink.Operation.PutFullData, Tilelink.Operation.PutPartialData) && io.tilelink.a.bits.address(1,0) === "b00".U && io.tilelink.a.bits.size === 2.U

        writeBus.valid := write
        io.sram.writeEnable := write
        io.sram.outputEnable := !write

      }

    }
    is(State.Receive) {


      io.sram.address := reqPipe.address(10,2) ## 1.B
      writeBus.bits := reqPipe.data(3) ## reqPipe.data(2)

      val write = reqPipe.opcode.isOneOf(Tilelink.Operation.PutFullData, Tilelink.Operation.PutPartialData) && reqPipe.address(1,0) === "b00".U && reqPipe.size === 2.U

      writeBus.valid := write
      io.sram.writeEnable := write
      io.sram.outputEnable := !write

      dataReg := readBus

      stateReg := State.Respond

    }
    is(State.Respond) {

      io.tilelink.d.valid := 1.B
      io.sram.outputEnable := 1.B


      stateReg := State.Idle

    }
  }


}
