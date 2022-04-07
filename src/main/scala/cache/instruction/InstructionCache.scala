package cache.instruction

import Chisel.ValidIO
import cache.Cache
import chisel3._
import chisel3.util.{DecoupledIO, isPow2, log2Ceil}
import lib.Types._

object InstructionCache {



  class Request(implicit dim: Cache.Dimension) extends Bundle {
    val valid = Input(Bool())
    val ready = Output(Bool())
    val address = Input(UInt(dim.Widths.address.W))
  }
  class Response extends Bundle {
    val valid = Output(Bool())
    val instruction = Output(Word())
  }
  class FillInterface(implicit dim: Cache.Dimension) extends Bundle {
    val fill = Output(Bool())
    val address = Output(UInt(dim.Widths.address.W))
    val length = Output(UInt(dim.Widths.blockOffset.W))

    val valid = Input(Bool())
    val data = Input(Word())
  }
  class IO(implicit dim: Cache.Dimension) extends Bundle {
    val request = new InstructionCache.Request
    val response = new InstructionCache.Response
    val fillPort = new InstructionCache.FillInterface
  }

}

abstract class InstructionCache(dim: Cache.Dimension) extends Module {

  val io = IO(new InstructionCache.IO()(dim))

}
