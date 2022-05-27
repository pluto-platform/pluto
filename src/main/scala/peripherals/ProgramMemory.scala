package peripherals

import charon.Tilelink
import charon.Tilelink.Operation
import chisel3._
import chisel3.util.log2Ceil
import lib.util.{BundleItemAssignment, ByteSplitter, Delay}

object ProgramMemory {
  def apply(words: Seq[BigInt]): ProgramMemory = Module(new ProgramMemory(words))
}

class ProgramMemory(words: Seq[BigInt]) extends Module {

  val io = IO(new Bundle {
    val tilelink = Tilelink.Agent.Interface.Responder(Tilelink.Parameters(4, log2Ceil(words.length*4), 2, Some(10), Some(10)))
  })

  val mem = VecInit(words.map(_.U(32.W)))
  val addrPipe = RegNext(io.tilelink.a.bits.address(log2Ceil(words.length*4)-1, 2), 0.U)
  val memOut = mem(addrPipe)
  val memOutPipe = RegInit(0.U(32.W))
  memOutPipe := memOut

  val reqPipe = Delay(io.tilelink.a.bits, 2)
  val validPipe = Delay(io.tilelink.a.valid, 2)

  io.tilelink.a.ready := 1.B
  io.tilelink.d.valid := validPipe

  io.tilelink.d.bits.set(
    _.opcode := Mux(reqPipe.opcode === Operation.Get, Tilelink.Response.AccessAckData, Tilelink.Response.AccessAck),
    _.param := 0.U,
    _.size := 2.U,
    _.source := reqPipe.source,
    _.sink := 0.U,
    _.denied := reqPipe.address(1,0) =/= 0.U || reqPipe.opcode =/= Operation.Get,
    _.data := memOutPipe.toBytes(4),
    _.corrupt := 0.B,
  )

}
