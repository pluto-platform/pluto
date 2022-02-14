package core

import chisel3._

object Branching {
  class FetchChannel extends Bundle {
    val takeGuess = Output(Bool())
    val jump = Output(Bool())
    val pc = Output(UInt(32.W))
    val offset = Output(SInt(32.W))
    val isTakingBranch = Input(Bool())
  }
  class DecodeChannel extends Bundle {
    val jump = Bool()
    val base = UInt(32.W)
    val offset = SInt(32.W)
  }
  class ExecutionChannel extends Bundle {
    val takeBranch = Bool()
    val target = UInt(32.W)
  }
}
