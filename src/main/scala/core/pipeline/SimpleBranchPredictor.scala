package core.pipeline

import chisel3._
import core.Branching

class SimpleBranchPredictor extends Module {

  val io = IO(new Branching.BranchPredictionChannel)

  io.guess := io.backwards

}
