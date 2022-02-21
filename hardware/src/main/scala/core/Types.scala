package core

import chisel3._

object Branching {
  class FetchChannel extends Bundle {
    val takeGuess = Output(Bool())
    val jump = Output(Bool())
    val target = Output(UInt(32.W))
    val guess = Input(Bool())
  }
  class DecodeChannel extends Bundle {
    val jump = Bool()
    val target = UInt(32.W)
  }
}
