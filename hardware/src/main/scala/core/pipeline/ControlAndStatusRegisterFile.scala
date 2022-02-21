package core.pipeline

import chisel3._

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

class ControlAndStatusRegisterFile {

}
