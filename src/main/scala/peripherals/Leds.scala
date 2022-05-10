package peripherals

import charon.Tilelink
import charon.Tilelink.Operation
import chisel3._
import lib.util.{BundleItemAssignment, ByteSplitter, SeqConcat}

class Leds(n: Int) extends Module {

  val io = IO(new Bundle {
    val leds = Output(UInt(n.W))
    val tilelink = Tilelink.Agent.Interface.Responder(
      Tilelink.Parameters(4, 1, 2, Some(10), Some(10))
    )
  })

  val reg = RegInit(1.U(n.W))
  io.leds := reg

  val reqPipe = RegNext(io.tilelink.a.bits)
  val validPipe = RegNext(io.tilelink.a.valid)
  io.tilelink.a.ready := 1.B

  io.tilelink.d.valid := validPipe
  io.tilelink.d.bits.set(
    _.opcode := Tilelink.Response.answer(reqPipe.opcode),
    _.param := 0.U,
    _.size := 2.U,
    _.source := reqPipe.source,
    _.sink := 0.U,
    _.denied := 0.B,
    _.data := reg.toBytes(4),
    _.corrupt := 0.B
  )

  when(io.tilelink.a.valid && io.tilelink.a.bits.opcode.isOneOf(Operation.PutFullData, Operation.PutPartialData)) {
    reg := io.tilelink.a.bits.data.concat
  }


}
