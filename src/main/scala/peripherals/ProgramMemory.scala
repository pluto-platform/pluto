package peripherals

import charon.Tilelink
import charon.Tilelink.Operation
import chisel3._
import chisel3.util.{MuxLookup, log2Ceil}
import lib.util.{BundleItemAssignment, ByteSplitter, Delay, SeqToVecMethods}

object ProgramMemory {
  def apply(bytes: Seq[Int]): ProgramMemory = Module(new ProgramMemory(bytes))
}

class ProgramMemory(bytes: Seq[Int]) extends Module {

  val addressWidth = log2Ceil(bytes.length)

  val io = IO(new Bundle {
    val tilelink = Tilelink.Agent.Interface.Responder(Tilelink.Parameters(4, addressWidth, 2, Some(10), Some(10)))
  })

  val mem = bytes
    .grouped(4)
    .map(_.map(_.U(8.W)).toVec)
    .toSeq
    .toVec

  val rowAddressPipe = RegNext(io.tilelink.a.bits.address(addressWidth - 1, 2), 0.U)
  val columnAddressPipe = RegNext(io.tilelink.a.bits.address(1,0), 0.U)
  val byteWiseRowPipe = RegNext(mem(rowAddressPipe), Seq.fill(4)(0.U).toVec)
  val halfwordWiseRow = VecInit(byteWiseRowPipe(1) ## byteWiseRowPipe(0), byteWiseRowPipe(3) ## byteWiseRowPipe(2))
  val word = byteWiseRowPipe(3) ## byteWiseRowPipe(2) ## byteWiseRowPipe(1) ## byteWiseRowPipe(0)
  val memOutPipe = MuxLookup(io.tilelink.a.bits.size, byteWiseRowPipe(columnAddressPipe), Seq(
    1.U -> halfwordWiseRow(columnAddressPipe(1)),
    2.U -> word
  ))

  val reqPipe = Delay(io.tilelink.a.bits, 3)
  val validPipe = Delay(io.tilelink.a.valid, 3)

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
