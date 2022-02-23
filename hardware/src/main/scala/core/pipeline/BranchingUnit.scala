package core.pipeline

import chisel3._
import core.Branching
import lib.util.BundleItemAssignment

class BranchingUnit extends Module {

  val io = IO(new Bundle {
    val fetch = Flipped(new Branching.FetchChannel)
    val decode = Flipped(new Branching.DecodeChannel)
    val pc = Flipped(new Branching.ProgramCounterChannel)
    val predictor = Flipped(new Branching.BranchPredictionChannel)
  })


  when(io.decode.decision) {
    io.pc.jump :=
    when(!io.decode.guess)
  }

}
