package plutocore.pipeline

import chisel3._
import chisel3.util._

object ControlAndStatusRegisterFile {

  class WriteRequest extends Bundle {
    val index = UInt(12.W)
    val value = UInt(32.W)
  }
  class ReadRequest extends Bundle {
    val index = UInt(12.W)
  }
  class ReadResponse extends Bundle {
    val value = UInt(32.W)
  }

}

class ControlAndStatusRegisterFile extends Module {

  val io = IO(new Bundle {
    val readRequest = Flipped(Valid(new ControlAndStatusRegisterFile.ReadRequest))
    val readResponse = Output(new ControlAndStatusRegisterFile.ReadResponse)
    val writeRequest = Flipped(Valid(new ControlAndStatusRegisterFile.WriteRequest))
  })

  val mem = SyncReadMem(1024,UInt(32.W))

  io.readResponse.value := mem.read(io.readRequest.bits.index, io.readRequest.valid)
  when(io.writeRequest.valid) {
    mem.write(io.writeRequest.bits.index, io.writeRequest.bits.value)
  }

}
