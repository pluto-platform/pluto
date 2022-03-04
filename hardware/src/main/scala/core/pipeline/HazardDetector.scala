package core.pipeline

import chisel3._
import core.Hazard
import lib.util.{BoolVec, Delay}

class HazardDetector extends Module {

  val io = IO(new Bundle {
    val fetch = Flipped(new Hazard.FetchChannel)
    val decode = Flipped(new Hazard.DecodeChannel)
  })

  val sourceMatch = io.fetch.source.map(_ === io.decode.destination).orR

  val loadUseHazard = sourceMatch && io.decode.isLoad
  val branchJumpHazard = sourceMatch && io.fetch.isBranchOrJalr

  io.decode.hazard := Delay(io.decode.canForward && (loadUseHazard || branchJumpHazard), cycles = 1)

}
