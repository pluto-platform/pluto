package cache.data

import lib.Types._
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.{Decoupled, Valid}
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryOperation}

object DataCache {
  class Request extends Bundle {
    val address = Word()
    val operation = MemoryOperation()
    val writeData = Vec(4, Byte())
    val byteEnable = Vec(4, Bool())
  }
  class Response extends Bundle {
    val readData = Vec(4, Byte())
    val result = MemoryAccessResult()
  }
}

abstract class DataCache extends Module {

  val io = IO(new Bundle {
    val request = Flipped(Decoupled(new DataCache.Request))
    val response = Valid(new DataCache.Response)
  })

}
