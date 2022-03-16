package plutocore.pipeline

import chisel3._
import chisel3.util.Valid
import lib.util.BundleItemAssignment
import plutocore.branchpredictor.BranchPrediction
object Branching {
  class FetchChannel extends Bundle {
    val takeGuess = Output(Bool())
    val jump = Output(Bool())
    val target = Output(UInt(32.W))
    val backwards = Output(Bool())
    val pc = Output(UInt(32.W))
    val nextPc = Output(UInt(32.W))
    val guess = Input(Bool())
  }
  class DecodeChannel extends Bundle {
    val jump = Output(Bool())
    val branch = Output(Bool())
    val guess = Output(Bool())
    val decision = Output(Bool())
    val target = Output(UInt(32.W))
    val pc = Output(UInt(32.W))
  }
  class ProgramCounterChannel extends Bundle {
    val next = Input(UInt(32.W))
  }
}
class BranchingUnit extends Module {

  val io = IO(new Bundle {
    val fetch = Flipped(new Branching.FetchChannel)
    val decode = Flipped(new Branching.DecodeChannel)
    val pc = Flipped(new Branching.ProgramCounterChannel)
    val predictor = Flipped(new BranchPrediction.BranchingUnitChannel)
  })

  val guess = io.fetch.takeGuess && io.predictor.guess
  val incorrectGuess = io.decode.decision =/= io.decode.guess

  io.predictor.set(
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
