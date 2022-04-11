package cache.instruction

import cache.Cache
import cache.Cache.LineInfo
import cache.Cache.AddressSplitter
import cache.instruction.DirectMapped.State
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import lib.Types.Word
import lib.util._

object DirectMapped {
  object State extends ChiselEnum {
    val Hit, Miss, Fill = Value
  }
}

class DirectMapped(val dim: Cache.Dimension) extends InstructionCache(dim) {
  implicit val d = dim
  import dim.{lines, blockSize, wordsPerLine, Widths}

  val blocks = SyncReadMem(lines, Vec(wordsPerLine, Word()))
  val metas = SyncReadMem(lines, LineInfo())

  val stateReg = RegInit(State.Hit)


  val addressReg = RegInit(0.U(Widths.address.W))
  val index = Mux(stateReg === State.Hit, io.request.bits.address.getIndex, addressReg.getIndex)
  val fillPointerReg = RegInit((1.B +: Seq.fill(wordsPerLine - 1)(0.B)).toVec)
  val requestPipe = RegNext(io.request.valid, 0.B)

  val meta = metas.read(index)
  val hit = meta.tag === addressReg.getTag && meta.valid
  when(stateReg === State.Hit && (hit || !requestPipe)) { addressReg := io.request.bits.address }

  val blockOffset = io.request.bits.address.getBlockOffset
  val wordSelectorReg = RegEnable( // register a one-hot version of the block offset if cache is operating normally
    UIntToOH(blockOffset).asBools.toVec,
    Seq.fill(wordsPerLine)(0.B).toVec,
    stateReg === State.Hit && (hit || !requestPipe)
  )
  io.response.bits.instruction := blocks.read(index).reduceWithOH(wordSelectorReg)


  io.fillPort.address := addressReg.getTag ## addressReg.getIndex ## Fill(Widths.blockOffset + 2, 0.B)
  io.fillPort.length := wordsPerLine.U
  io.fillPort.fill := 0.B

  io.request.ready := 0.B
  io.response.valid := 0.B

  switch(stateReg) {
    is(State.Hit) {
      stateReg := Mux(!hit && requestPipe, State.Miss, State.Hit)
      io.request.ready := (requestPipe && hit) || !requestPipe
      io.response.valid := requestPipe && hit
    }
    is(State.Miss) {
      stateReg := State.Fill
      io.request.ready := 0.B
      io.response.valid := 0.B

      metas.write(addressReg.getIndex, LineInfo(1.B, addressReg.getTag))

    }
    is(State.Fill) {
      stateReg := State.Fill
      io.fillPort.fill := 1.B
      io.request.ready := 0.B
      io.response.valid := 0.B

      when(io.fillPort.valid) {

        when(fillPointerReg.last) {
          requestPipe := 1.B
          stateReg := State.Hit
        }

        fillPointerReg := fillPointerReg.rotatedLeft
        blocks.write(
          addressReg.getIndex,
          Seq.fill(wordsPerLine)(io.fillPort.data).toVec,
          fillPointerReg
        )

      }

    }
  }

}

object Emit extends App {
  emitVerilog(new DirectMapped(Cache.Dimension(1024, 8, BigInt(0) until BigInt(0x100000))))
}