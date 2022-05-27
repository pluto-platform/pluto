package peripherals

import charon.Tilelink
import chisel3._
import chisel3.experimental.{Analog, attach}
import lib.modules.TriStateDriver
import lib.util.{BundleItemAssignment, ByteSplitter}

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

}

class Sram extends Module {

  val io = IO(new Bundle {
    val tilelink = Tilelink.Agent.Interface.Responder(Tilelink.Parameters(4, 20, 2, Some(10), Some(10)))
    val sram = new Sram.IO
  })


  val (readBus, writeBus) = TriStateDriver(io.sram.data)

  io.sram.set(
    _.strobe := VecInit(0.B, 1.B),
    _.outputEnable := 1.B,
    _.chipSelect := 0.B,
    _.address := io.tilelink.a.bits.address
  )

  val reqPipe = RegNext(io.tilelink.a.bits, 0.U.asTypeOf(io.tilelink.a.bits))
  val validPipe = RegNext(io.tilelink.a.valid, 0.B)

  val write = io.tilelink.a.valid && io.tilelink.a.bits.opcode.isOneOf(Tilelink.Operation.PutFullData, Tilelink.Operation.PutPartialData) && io.tilelink.a.bits.size === 0.U
  io.sram.writeEnable := write
  writeBus.set(
    _.valid := write,
    _.bits := io.tilelink.a.bits.data(0)
  )

  io.tilelink.a.ready := 1.B
  io.tilelink.d.valid := validPipe

  io.tilelink.d.bits.set(
    _.opcode := Mux(reqPipe.opcode === Tilelink.Operation.Get, Tilelink.Response.AccessAckData, Tilelink.Response.AccessAck),
    _.param := 0.U,
    _.size := 0.U,
    _.source := reqPipe.source,
    _.sink := 0.U,
    _.denied := reqPipe.size > 0.U,
    _.data := readBus.toBytes(4),
    _.corrupt := 0.B
  )


}
