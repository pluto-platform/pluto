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

object Forwarding {
  class ExecuteChannel extends Bundle {
    val source = Output(Vec(2, UInt(5.W)))
    val shouldForward = Input(Vec(2, Bool()))
    val value = Input(UInt(32.W))
  }
  class ProviderChannel extends Bundle {
    val destination = Output(UInt(5.W))
    val value = Output(UInt(32.W))
  }
}
