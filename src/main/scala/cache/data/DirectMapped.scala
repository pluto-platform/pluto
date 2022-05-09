package cache.data

import cache.Cache
import cache.Cache.{AddressSplitter, LineInfo}
import cache.data.DirectMapped.State
import charon.Tilelink
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.{Counter, Fill, MuxLookup, RegEnable, UIntToOH, is, log2Ceil, switch}
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryAccessWidth, MemoryOperation}
import lib.LookUp._
import lib.Types.Byte
import lib.util.{BoolVec, BundleItemAssignment, ByteSplitter, DataReducer, SeqConcat, SeqToVecMethods}

object DirectMapped {
  object State extends ChiselEnum {
    val Hit, Miss, Fill = Value
  }
}

class DirectMapped(val dim: Cache.Dimension) extends DataCache(dim) {
  implicit val d = dim
  import dim.{lines, blockSize, wordsPerLine, Widths}

  val blocks = SyncReadMem(lines, Vec(wordsPerLine * 4, Byte()))
  val metas = SyncReadMem(lines, LineInfo())

  val stateReg = RegInit(State.Hit)

  val addressReg = RegInit(0.U(Widths.address.W))
  val index = Mux(stateReg === State.Hit, io.request.bits.address.getIndex, addressReg.getIndex)
  val fillPointerReg = RegInit((1.B +: Seq.fill(wordsPerLine - 1)(0.B)).toVec)
  val requestPipe = RegNext(io.request.valid, 0.B)

  io.tilelink.a.set(
    _.valid := 0.B,
    _.bits.set(
      _.opcode := Tilelink.Operation.PutFullData,
      _.param := 0.U,
      _.size := 2.U,
      _.source := 0.U,
      _.address := io.request.bits.address,
      _.mask := io.request.bits.byteEnable,
      _.data := io.request.bits.writeData,
      _.corrupt := 0.B
    )
  )
  io.tilelink.d.ready := 1.B


  val addressCounter = Counter(wordsPerLine)
  val sendBurst = RegInit(0.B)
  when(sendBurst) {
    io.tilelink.a.valid := 1.B
    io.tilelink.a.bits.address := io.request.bits.address.getTag ## io.request.bits.address.getIndex ## addressCounter.value ## 0.U(2.W)
    when(io.tilelink.a.ready) {
      when(addressCounter.inc()) {
        sendBurst := 0.B
      }
    }

  }


  val meta = metas.read(index)
  val hit = meta.tag === addressReg.getTag && meta.valid
  when(stateReg === State.Hit && (hit || !requestPipe)) { addressReg := io.request.bits.address }

  val opPipe = RegEnable( // register a one-hot version of the block offset if cache is operating normally
    io.request.bits.operation,
    MemoryOperation.Read,
    stateReg === State.Hit && (hit || !requestPipe)
  )
  val sizePipe = RegEnable( // register a one-hot version of the block offset if cache is operating normally
    io.request.bits.size,
    MemoryAccessWidth.Byte,
    stateReg === State.Hit && (hit || !requestPipe)
  )

  val blockOffset = io.request.bits.address.getBlockOffset
  val byteOffset = io.request.bits.address.getByteOffset
  val wordSelectorReg = RegEnable(
    UIntToOH(blockOffset).asBools.toVec,
    Seq.fill(wordsPerLine)(0.B).toVec,
    stateReg === State.Hit && (hit || !requestPipe)
  )
  val halfwordSelectorReg = RegEnable(
    UIntToOH(blockOffset ## byteOffset(1)).asBools.toVec,
    Seq.fill(wordsPerLine * 2)(0.B).toVec,
    stateReg === State.Hit && (hit || !requestPipe)
  )
  val byteSelectorReg = RegEnable(
    UIntToOH(blockOffset ## byteOffset).asBools.toVec,
    Seq.fill(wordsPerLine * 4)(0.B).toVec,
    stateReg === State.Hit && (hit || !requestPipe)
  )

  val bytes = blocks.read(index)
  val halfwords = bytes
    .grouped(2)
    .map(_.concat)
    .toSeq.toVec
  val words = bytes
    .grouped(4)
    .map(_.concat)
    .toSeq.toVec

  val byte = bytes.reduceWithOH(byteSelectorReg)
  val halfword = halfwords.reduceWithOH(halfwordSelectorReg)
  val word = words.reduceWithOH(wordSelectorReg)

  val readData = lookUp(sizePipe) in (
    MemoryAccessWidth.Byte -> byte.toBytes(4).toVec,
    MemoryAccessWidth.HalfWord -> halfword.toBytes(4).toVec,
    MemoryAccessWidth.Word -> word.toBytes(4).toVec
  )

  io.request.ready := 0.B
  io.response.valid := 0.B
  io.response.bits.set(
    _.result := MemoryAccessResult.Success,
    _.readData := readData
  )


  when(io.request.valid && io.request.bits.operation === MemoryOperation.Write) {

    val writeMask = lookUp(io.request.bits.size) in (
      MemoryAccessWidth.Byte -> UIntToOH(blockOffset ## byteOffset).asBools.toVec,
      MemoryAccessWidth.HalfWord -> UIntToOH(blockOffset ## byteOffset(1)).asBools.flatMap(b => VecInit(b,b)).toVec,
      MemoryAccessWidth.Word -> UIntToOH(blockOffset).asBools.flatMap(b => VecInit(b,b,b,b)).toVec
    )

    blocks.write(index,Seq.fill(wordsPerLine)(io.request.bits.writeData).flatten.toVec, writeMask)

  }

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

      sendBurst := 1.B

      metas.write(addressReg.getIndex, LineInfo(1.B, addressReg.getTag))

    }
    is(State.Fill) {
      stateReg := State.Fill

      io.request.ready := 0.B
      io.response.valid := 0.B

      when(io.tilelink.d.valid) {

        when(fillPointerReg.last) {
          requestPipe := 1.B
          stateReg := State.Hit
        }

        fillPointerReg := fillPointerReg.rotatedLeft
        blocks.write(
          addressReg.getIndex,
          Seq.fill(wordsPerLine)(io.tilelink.d.bits.data).flatten.toVec,
          fillPointerReg.flatMap(b => VecInit(b, b, b, b)).toVec
        )

      }

    }
  }

}
