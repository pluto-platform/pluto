package peripherals

import charon.Tilelink
import charon.Tilelink.Operation
import chisel3._
import chisel3.util.log2Ceil
import lib.Types.Byte
import lib.util.{BundleItemAssignment, ByteSplitter, Delay, SeqConcat, SeqToVecMethods}

object BlockRam {
  def apply(size: Int): BlockRam = Module(new BlockRam(size))
}

class BlockRam(size: Int) extends Module {

  val io = IO(new Bundle {
    val tilelink = Tilelink.Agent.Interface.Responder(Tilelink.Parameters(4, log2Ceil(size), 2, Some(10), Some(10)))
  })

  val mem = SyncReadMem(size/4, Vec(4, Byte()))
  val memOut = mem.read(io.tilelink.a.bits.address(log2Ceil(size)-1, 2))
  val memOutPipe = RegNext(memOut, Seq.fill(4)(0.U).toVec)

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
    _.denied := reqPipe.address(1,0) =/= 0.U,
    _.data := memOutPipe,
    _.corrupt := 0.B,
  )

  when(io.tilelink.a.valid && io.tilelink.a.bits.opcode === Tilelink.Operation.PutPartialData) {
    mem.write(io.tilelink.a.bits.address(log2Ceil(size)-1, 2), io.tilelink.a.bits.data, io.tilelink.a.bits.mask)
  }

}
