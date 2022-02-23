package core.pipeline

import chisel3._
import core.LoadUseHazard

class LoadUseHazardDetector extends Module {

  val io = IO(new Bundle {
    val decode = Flipped(new LoadUseHazard.DecodeChannel)
    val execute = Flipped(new LoadUseHazard.ExecuteChannel)
  })

  io.decode.hazard := io.decode.source.map(_ === io.execute.destination).reduce(_||_) && io.execute.isLoad

}
