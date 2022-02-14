package core.pipeline

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.{Valid, ValidIO}

object IntegerRegisterFile {

  class SourceRequest extends Bundle {
    val index = Vec(2,UInt(5.W))
  }
  class SourceResponse extends Bundle {
    val data = Vec(2, UInt(32.W))
  }

  class WriteRequest extends Bundle {
    val index = UInt(5.W)
    val data = UInt(32.W)
  }

}

class IntegerRegisterFile extends Module {

  val io = IO(new Bundle {

    val source = new Bundle {
      val request = Input(new IntegerRegisterFile.SourceRequest)
      val response = Output(new IntegerRegisterFile.SourceResponse)
    }
    val write = Flipped(Valid(new IntegerRegisterFile.WriteRequest))

  })

  val memory = SyncReadMem(32, UInt(32.W))

  val writeIsNotX0 = io.write.bits.index.orR

  io.source.request.index.zip(io.source.response.data).foreach { case (address, data) =>
    data := Mux(
      io.write.valid && writeIsNotX0 && address === io.write.bits.index,
      io.write.bits.data,
      memory.read(address)
    )
  }

  when(io.write.valid && writeIsNotX0) {
    memory.write(io.write.bits.index, io.write.bits.data)
  }

}
