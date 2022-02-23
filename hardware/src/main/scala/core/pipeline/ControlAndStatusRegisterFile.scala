package core.pipeline

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
    val readResponse = new ControlAndStatusRegisterFile.ReadResponse
    val writeRequest = Flipped(Valid(new ControlAndStatusRegisterFile.WriteRequest))
  })

  io.readResponse.value := 0.U

}
