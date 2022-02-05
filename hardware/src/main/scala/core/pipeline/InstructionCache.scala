package core.pipeline

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import core.pipeline.InstructionCache.State


object InstructionCache {

  object State extends ChiselEnum {
    val Hit, IssueFetch, WaitForBlock, WriteBlock = Value
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

  class MemoryInterface(dim: CacheDimension) extends DecoupledIO(new Bundle {
    val address = Output(UInt(32.W))
    val length = Output(UInt(dim.blockOffsetWidth.W))
    val readData = Input(UInt(32.W))
  })


}

class InstructionCache(dim: InstructionCache.CacheDimension) extends Module {



  val io = IO(new Bundle {

    val request = Flipped(new InstructionCache.RequestPort)

    val response = new InstructionCache.ResponsePort

    val memory = new InstructionCache.MemoryInterface(dim)

    val invalidate = Input(Bool())

  })

  val selectPipedAddress = WireDefault(0.B)
  val addressPipe = RegInit(0.U(32.W))
  val request = Mux(selectPipedAddress,addressPipe,io.request.bits.address).asTypeOf(new InstructionCache.Request(dim))

  val stateReg = RegInit(State.Hit)

  val validReg = RegInit(VecInit(Seq.fill(dim.lines)(0.B)))
  val tagMem = SyncReadMem(dim.lines, UInt(dim.tagWidth.W))
  val blockMem = SyncReadMem(dim.lines, Vec(dim.blockSize * 4, UInt(8.W)))

  val valid = validReg(request.index)
  val tag = tagMem.read(request.index)
  val block = blockMem.read(request.index)
  val word = block(request.blockOffset ## request.byteOffset)

  val hit = valid && tag === request.tag



  io.memory.valid := 0.B
  io.memory.bits.address := addressPipe
  io.memory.bits.length := dim.blockSize.U

  switch(stateReg) {
    io.response.valid := hit
    is(State.Hit) {
      stateReg := Mux(hit, State.Hit, State.IssueFetch)
    }
    is(State.IssueFetch) {
      io.response.valid := 0.B
      io.memory.valid := 1.B
      stateReg := State.WaitForBlock
    }
    is(State.WaitForBlock) {
      io.response.valid := 0.B
      when(io.memory.)
    }
  }



}
