package cache.data

import cache.Cache
import charon.Tilelink
import lib.Types._
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.{Decoupled, DecoupledIO, Valid, ValidIO}
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryOperation}

object DataCache {
  class Request extends Bundle {
    val address = Word()
    val operation = MemoryOperation()
    val writeData = Vec(4, Byte())
    val byteEnable = Vec(4, Bool())
  }
  class RequestIO extends DecoupledIO(new Request)
  class Response extends Bundle {
    val readData = Vec(4, Byte())
    val result = MemoryAccessResult()
  }
  class ResponseIO extends ValidIO(new Response)
  class IO(dim: Cache.Dimension) extends Bundle {
    val request = Flipped(Decoupled(new DataCache.Request))
    val response = Valid(new DataCache.Response)
    val tilelink = Tilelink.Agent.Interface.Requester(Tilelink.Parameters(4, 32, 2, Some(10), Some(10)))
  }
}

abstract class DataCache(dim: Cache.Dimension) extends Module {

  val io = IO(new DataCache.IO(dim))

}
