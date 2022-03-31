package cores.modules.cache

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import InstructionCache.State


object InstructionCache {

  object State extends ChiselEnum {
    val Hit, IssueFetch, WriteBlock, FinishRequest = Value
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

  class MemoryInterface(dim: CacheDimension) extends Bundle {
    val request = Output(Bool())
    val address = Output(UInt(32.W))
    val length = Output(UInt(dim.blockOffsetWidth.W))
    val readData = Input(UInt(32.W))
    val transfering = Input(Bool())
  }


}

class InstructionCache(dim: InstructionCache.CacheDimension) extends Module {



  val io = IO(new Bundle {

    val request = Flipped(new InstructionCache.RequestPort)

    val response = new InstructionCache.ResponsePort

    val memory = new InstructionCache.MemoryInterface(dim)

    val invalidate = Input(Bool())

  })

  val stateReg = RegInit(State.Hit)

  val addressPipe = RegInit(0.U(32.W))
  when(stateReg === State.Hit) { addressPipe := io.request.bits.address }
  val request = Mux(stateReg === State.Hit,addressPipe,io.request.bits.address).asTypeOf(new InstructionCache.Request(dim))

  val writeOffsetCounter = RegInit(1.U(dim.blockSize.W))

  val validReg = RegInit(VecInit(Seq.fill(dim.lines)(0.B)))
  when(io.invalidate) { validReg := 0.U(dim.lines.W).asBools }
  val tagMem = SyncReadMem(dim.lines, UInt(dim.tagWidth.W))
  val blockMem = SyncReadMem(dim.lines, Vec(dim.blockSize, UInt(32.W)))

  val valid = validReg(RegNext(request.index))
  val tag = tagMem.read(request.index)
  val block = blockMem.read(request.index)
  val wordOfInterest = block(request.blockOffset)

  io.request.ready := 1.B
  io.response.bits.instruction := wordOfInterest

  val hit = valid && (tag === request.tag)

  io.memory.request := 0.B
  io.memory.address := addressPipe.head(dim.tagWidth + dim.indexWidth) ## 0.U((dim.blockOffsetWidth + 2).W)
  io.memory.length := dim.blockSize.U

  io.response.valid := hit

  switch(stateReg) {

    is(State.Hit) {

      stateReg := Mux(hit, State.Hit, State.IssueFetch)

    }
    is(State.IssueFetch) {

      io.request.ready := 0.B
      io.response.valid := 0.B
      io.memory.request := 1.B

      writeOffsetCounter := 1.U

      tagMem.write(request.index, request.tag)
      validReg(request.index) := 1.B

      stateReg := State.WriteBlock

    }
    is(State.WriteBlock) {

      io.response.valid := 0.B
      io.request.ready := 0.B
      io.memory.request := 0.B

      writeOffsetCounter := writeOffsetCounter << 1

      when(io.memory.transfering) {
        blockMem.write(
          request.index,
          VecInit(Seq.fill(dim.blockSize)(io.memory.readData)),
          writeOffsetCounter.asBools
        )
      }

      when(writeOffsetCounter(dim.blockSize - 1)) { stateReg := State.Hit }

    }
  }



}
