package cache.data

import cache.Cache
import charon.Tilelink
import lib.Types._
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.{Decoupled, DecoupledIO, Valid}
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryOperation}
import lib.util.InputOutputExtender

object DataCache {
  class Request extends Bundle {
    val address = Word()
    val operation = MemoryOperation()
    val writeData = Vec(4, Byte())
    val byteEnable = Vec(4, Bool())
  }
  class RequestIO(implicit dim: Cache.Dimension) extends DecoupledIO(new Request)
  class Response extends Bundle {
    val readData = Vec(4, Byte())
    val result = MemoryAccessResult()
  }
  class ResponseIO(implicit dim: Cache.Dimension) extends DecoupledIO(new Response)
  class IO(implicit dim: Cache.Dimension) extends Bundle {
    val request = new DataCache.RequestIO().flipped
    val response = new DataCache.ResponseIO
    val tilelink = Tilelink.Agent.Interface.Requester(Tilelink.Parameters(

    ))
  }
}

abstract class DataCache extends Module {

  val io = IO(new Bundle {
    val request = Flipped(Decoupled(new DataCache.Request))
    val response = Valid(new DataCache.Response)
  })

}
