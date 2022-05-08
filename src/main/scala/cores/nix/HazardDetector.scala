package cores.nix

import chisel3._
import lib.util.BoolVec

object Hazard {
  class DecodeChannel extends Bundle {
    val source = Output(Vec(2, UInt(5.W)))
    val isCsr = Output(Bool())
    val hazard = Input(Bool())
  }
  class ExecuteChannel extends Bundle {
    val destination = Output(UInt(5.W))
    val isLoad = Output(Bool())
    val canForward = Output(Bool())
    val isCsr = Output(Bool())
    val bubble = Output(Bool())
  }
  class MemoryChannel extends Bundle {
    val destination = Output(UInt(5.W))
    val canForward = Output(Bool())
    val isCsr = Output(Bool())
    val bubble = Output(Bool())
  }
}

class HazardDetector extends Module {

  val io = IO(new Bundle {
    val decode = Flipped(new Hazard.DecodeChannel)
    val execute = Flipped(new Hazard.ExecuteChannel)
    val memory = Flipped(new Hazard.MemoryChannel)
  })

  val sourceMatchExecute = io.decode.source.map(_ === io.execute.destination && io.execute.canForward)
  val sourceMatchMemory = io.decode.source.map(_ === io.memory.destination && io.memory.canForward)

  val loadUseHazard = sourceMatchExecute.orR && io.execute.isLoad
  val csrHazard = io.decode.isCsr && (io.execute.isCsr || io.memory.isCsr)

  io.decode.hazard := loadUseHazard || csrHazard || io.execute.bubble || io.memory.bubble

}
