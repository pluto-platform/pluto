package core

import chisel3._

class StageInterface extends Bundle {
  val stall = Output(Bool())
  val flush = Output(Bool())
}

abstract class PipelineStage[UP <: StageInterface, DOWN <: StageInterface](up: => UP, down: => DOWN) extends Module {

  val upstream = IO(Input(up))
  val downstream = IO(Output(down))



  def :>[S <: PipelineStage[DOWN,_]](that: S): S = {
    downstream <> that.upstream
    that
  }
}