package cache.instruction

import Chisel.ValidIO
import cache.Cache
import chisel3._
import chisel3.util.{DecoupledIO, isPow2, log2Ceil}
import lib.Types._

object InstructionCache {



  class Request(implicit dim: Cache.Dimension) extends Bundle {
    val address = UInt(dim.Widths.address.W)
  }
  class RequestIO(implicit dim: Cache.Dimension) extends DecoupledIO(new Request)
  class Response extends Bundle {
    val instruction = Word()
  }
  class ResponseIO extends ValidIO(new Response)
  class FillIO(implicit dim: Cache.Dimension) extends Bundle {
    val fill = Output(Bool())
    val address = Output(UInt(dim.Widths.address.W))
    val length = Output(UInt((dim.Widths.blockOffset+1).W))

    val valid = Input(Bool())
    val data = Input(Word())
  }
  class IO(implicit dim: Cache.Dimension) extends Bundle {
    val request = Flipped(new InstructionCache.RequestIO)
    val response = new InstructionCache.ResponseIO
    val fillPort = new InstructionCache.FillIO
  }

}

abstract class InstructionCache(dim: Cache.Dimension) extends Module {

  val io = IO(new InstructionCache.IO()(dim))

}
