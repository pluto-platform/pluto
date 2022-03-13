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

  val guess = io.fetch.takeGuess && io.predictor.guess
  val incorrectGuess = io.decode.decision =/= io.decode.guess

  io.predictor.set(
    _.pc := io.fetch.pc,
    _.backwards := io.fetch.backwards,
    _.update.valid := incorrectGuess,
    _.update.bits.set(
      _.pc := io.decode.pc,
      _.decision := io.decode.decision
    )
  )

  io.fetch.guess := guess

  when(incorrectGuess || io.decode.jump) {
    io.pc.next := io.decode.target
  }.elsewhen(guess || io.fetch.jump) {
    io.pc.next := io.fetch.target
  } otherwise {
    io.pc.next := io.fetch.nextPc
  }

}
