package core.pipeline

import chisel3._
import core.Hazard
import lib.util.{BoolVec, Delay}

class HazardDetector extends Module {

  val io = IO(new Bundle {
    val fetch = Flipped(new Hazard.FetchChannel)
    val decode = Flipped(new Hazard.DecodeChannel)
  })

  val sourceMatch = io.fetch.source.map(_ === io.decode.destination)

  val loadUseHazard = sourceMatch.orR && io.decode.isLoad
  val branchDataHazard = sourceMatch.orR && io.fetch.isBranch
  val jumpDataHazard = sourceMatch(0) && io.fetch.isJalr

  io.decode.hazard := Delay(io.decode.canForward && (loadUseHazard || branchDataHazard || jumpDataHazard), cycles = 1)

}
