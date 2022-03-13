package plutocore.pipeline

import chisel3._
import lib.util.{BoolVec, Delay}

class HazardDetector extends Module {

  val io = IO(new Bundle {
    val decode = Flipped(new Hazard.DecodeChannel)
    val execute = Flipped(new Hazard.ExecuteChannel)
    val memory = Flipped(new Hazard.MemoryChannel)
  })

  val sourceMatchExecute = io.decode.source.map(_ === io.execute.destination)
  val sourceMatchMemory = io.decode.source.map(_ === io.execute.destination)

  val loadUseHazard = sourceMatchExecute.orR && io.execute.isLoad
  val loadUseHazardWithBranch = sourceMatchMemory.orR && io.memory.isLoad && io.decode.isBranch
  val loadUseHazardWithJump = sourceMatchMemory(0) && io.memory.isLoad && io.decode.isJalr
  val branchDataHazard = sourceMatchExecute.orR && io.decode.isBranch
  val jumpDataHazard = sourceMatchExecute(0) && io.decode.isJalr

  io.decode.hazard := (io.execute.canForward && (loadUseHazard || branchDataHazard || jumpDataHazard)) || (io.memory.canForward && (loadUseHazardWithBranch || loadUseHazardWithJump))

}
