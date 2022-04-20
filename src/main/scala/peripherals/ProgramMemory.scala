package peripherals

import charon.Tilelink
import charon.Tilelink.Operation
import chisel3._
import chisel3.util.log2Ceil
import lib.util.{BundleItemAssignment, ByteSplitter, Delay}

class ProgramMemory(words: Seq[BigInt]) extends Module {

  val io = IO(new Bundle {
    val tilelink = Tilelink.Agent.Interface.Responder(Tilelink.Parameters(4, log2Ceil(words.length), 2, Some(10), Some(10)))
  })

  val mem = VecInit(words.map(_.U(32.W)))
  val addrPipe = RegNext(io.tilelink.a.address(log2Ceil(words.length)-1, 2), 0.U)
  val memOut = mem(addrPipe)
  println(memOut.getWidth)
  val memOutPipe = RegInit(0.U(32.W))
  memOutPipe := memOut
  println(memOutPipe.getWidth)

  val reqPipe = Reg(Output(chiselTypeOf(io.tilelink.a)))
  reqPipe := io.tilelink.a
  val reqPipe2 = Reg(Output(chiselTypeOf(io.tilelink.a)))
  reqPipe2 := reqPipe

  io.tilelink.a.ready := 1.B

  io.tilelink.d.set(
    _.opcode := Mux(reqPipe2.opcode === Operation.Get, Tilelink.Response.AccessAckData, Tilelink.Response.AccessAck),
    _.param := 0.U,
    _.size := 2.U,
    _.source := reqPipe2.source,
    _.sink := 0.U,
    _.denied := reqPipe2.address(1,0) =/= 0.U || reqPipe2.opcode =/= Operation.Get,
    _.data := memOutPipe.toBytes(4),
    _.corrupt := 0.B,
    _.valid := reqPipe2.valid
  )

}
