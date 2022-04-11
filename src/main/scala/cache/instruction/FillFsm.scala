package cache.instruction

import cache.Cache
import chisel3._
import chisel3.util.log2Ceil
import lib.Types.Word

class FillFsm(dim: Cache.Dimension) extends Module {

  val io = IO(new Bundle {
    val fillreq = Flipped(new InstructionCache.FillIO()(dim))
    val mem = new Bundle {
      val address = Output(UInt(32.W))
      val req = Output(Bool())
      val data = Input(Word())
    }
  })

  val counter = RegInit(UInt(log2Ceil(dim.wordsPerLine).W), 0.U)

  val fetching = RegInit(0.B)

  when(io.fillreq.fill && !fetching) {
    fetching := 1.B
    counter := 0.U
  }

  io.mem.address := io.fillreq.address + (counter ## 0.U(2.W))
  io.mem.req := 0.B

  io.fillreq.valid := RegNext(fetching, 0.B)
  io.fillreq.data := io.mem.data

  when(fetching) {

    io.mem.req := 1.B
    when(counter < (io.fillreq.length - 1.U)) {
      counter := counter + 1.U
    } otherwise {
      fetching := 0.B
    }

  }


}
