package core.pipeline

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import core.pipeline.InstructionCache.State


object InstructionCache {

  object State extends ChiselEnum {
    val Hit, Miss = Value
  }

  case class CacheDimension(size: Int, blockSize: Int) {
    require(isPow2(size) && isPow2(blockSize))

    val lines = size / (blockSize * 4)

    val blockOffsetWidth = log2Ceil(blockSize)
    val indexWidth = log2Ceil(lines)
    val tagWidth = 30 - blockOffsetWidth - indexWidth

  }

  class RequestPort extends DecoupledIO(new Bundle {
    val address = Output(UInt(32.W))
  })

  class ResponsePort extends ValidIO(new Bundle {
    val instruction = Output(UInt(32.W))
  })

  class Request(dim: CacheDimension) extends Bundle {
    val tag = UInt(dim.tagWidth.W)
    val index = UInt(dim.indexWidth.W)
    val blockOffset = UInt(dim.blockOffsetWidth.W)
    val byteOffset = UInt(2.W)
  }


}

class InstructionCache(dim: InstructionCache.CacheDimension) extends Module {



  val io = IO(new Bundle {

    val request = Flipped(new InstructionCache.RequestPort)

    val response = new InstructionCache.ResponsePort

    val invalidate = Input(Bool())

  })

  val request = io.request.bits.address.asTypeOf(new InstructionCache.Request(dim))

  val stateReg = RegInit(State.Hit)

  val validReg = RegInit(VecInit(Seq.fill(dim.lines)(0.B)))
  val tagMem = SyncReadMem(dim.lines, UInt(dim.tagWidth.W))
  val blockMem = SyncReadMem(dim.lines, Vec(dim.blockSize * 4, UInt(8.W)))

  val valid = validReg(request.index)
  val tag = tagMem.read(request.index)
  val block = blockMem.read(request.index)



  switch(stateReg) {
    is(State.Hit) {

    }
    is(State.Miss) {

    }
  }



}
