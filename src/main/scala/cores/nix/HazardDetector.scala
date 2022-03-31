package cores.nix

import chisel3._
import lib.util.BoolVec

object Hazard {
  class DecodeChannel extends Bundle {
    val source = Output(Vec(2, UInt(5.W)))
    val hazard = Input(Bool())
  }
  class ExecuteChannel extends Bundle {
    val destination = Output(UInt(5.W))
    val isLoad = Output(Bool())
    val canForward = Output(Bool())
  }
}

class HazardDetector extends Module {

  val io = IO(new Bundle {
    val decode = Flipped(new Hazard.DecodeChannel)
    val execute = Flipped(new Hazard.ExecuteChannel)
  })

  val sourceMatchExecute = io.decode.source.map(_ === io.execute.destination)

  val loadUseHazard = sourceMatchExecute.orR && io.execute.isLoad

  io.decode.hazard := io.execute.canForward && loadUseHazard

}
