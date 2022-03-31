package cores.modules.cache.instruction

import Chisel.ValidIO
import chisel3._
import chisel3.util.DecoupledIO

object InstructionCache {

  class Request extends DecoupledIO(new Bundle {
    val address = UInt(32.W)
  })
  class Response extends ValidIO(new Bundle {
    val instruction = UInt(32.W)
  })

}

abstract class InstructionCache extends Module {

  val io = None

}
