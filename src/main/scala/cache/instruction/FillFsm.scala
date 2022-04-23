package cache.instruction

import cache.Cache
import charon.Tilelink
import chisel3._
import chisel3.util.log2Ceil
import lib.Types.Word
import lib.util.{BundleItemAssignment, SeqConcat, SeqToVecMethods, rising}

class FillFsm(dim: Cache.Dimension) extends Module {

  val io = IO(new Bundle {
    val fillreq = Flipped(new InstructionCache.FillIO()(dim))
    val tilelink = Tilelink.Agent.Interface.Requester(Tilelink.Parameters(4, dim.Widths.address, 2))
  })

  val counter = RegInit(UInt(log2Ceil(dim.wordsPerLine).W), 0.U)

  val fetching = RegInit(0.B)

  when(rising(io.fillreq.fill) && !fetching) {
    fetching := 1.B
    counter := 0.U
  }

  io.tilelink.a.bits.set(
    _.address := io.fillreq.address + (counter ## 0.U(2.W)),
    _.data := DontCare,
    _.source := 0.U,
    _.corrupt := 0.B,
    _.mask := Seq.fill(4)(0.B).toVec,
    _.param := 0.U,
    _.size := 2.U,
    _.opcode := Tilelink.Operation.Get
  )
  io.tilelink.a.valid := 0.B
  io.tilelink.d.ready := 1.B
  io.fillreq.valid := io.tilelink.d.valid
  io.fillreq.data := io.tilelink.d.bits.data.concat

  when(fetching) {

    io.tilelink.a.valid := 1.B
    when(io.tilelink.a.ready) {
      when(counter < (io.fillreq.length - 1.U)) {
        counter := counter + 1.U
      } otherwise {
        fetching := 0.B
      }
    }

  }


}
