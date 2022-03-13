package plutocore.pipeline

import chisel3._

class SimpleBranchPredictor extends Module {

  val io = IO(new Branching.BranchPredictionChannel)

  io.guess := io.backwards

}
