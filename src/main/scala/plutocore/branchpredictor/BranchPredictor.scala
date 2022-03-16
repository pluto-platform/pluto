package plutocore.branchpredictor

import chisel3._
import chisel3.util.Valid
import plutocore.pipeline.Branching


object BranchPrediction {
  class BranchingUnitChannel extends Bundle {
    val update = Flipped(Valid(new Bundle {
      val pc = UInt(32.W)
      val decision = Bool()
    }))
    val backwards = Input(Bool())
    val guess = Output(Bool())
  }
}

abstract class BranchPredictor extends Module {
  val io = IO(new Bundle {
    val pc = Input(UInt(32.W))
    val branchingUnit = new BranchPrediction.BranchingUnitChannel
  })
}
