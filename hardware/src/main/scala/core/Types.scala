package core

import chisel3._
import chisel3.util.Valid

object Branching {
  class FetchChannel extends Bundle {
    val takeGuess = Output(Bool())
    val jump = Output(Bool())
    val target = Output(UInt(32.W))
    val backwards = Output(Bool())
    val pc = Output(UInt(32.W))
    val guess = Input(Bool())
  }
  class DecodeChannel extends Bundle {
    val decision = Output(Bool())
    val jump = Output(Bool())
    val target = Output(UInt(32.W))
    val pc = Output(UInt(32.W))
    val guess = Output(Bool())
  }
  class ProgramCounterChannel extends Bundle {
    val target = Input(UInt(32.W))
    val jump = Input(Bool())
  }
  class BranchPredictionChannel extends Bundle {
    val update = Flipped(Valid(new Bundle {
      val pc = UInt(32.W)
      val decision = Bool()
    }))
    val pc = Input(UInt(32.W))
    val backwards = Input(Bool())
    val guess = Output(Bool())
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
    val destinationIsNonZero = Output(Bool())
    val value = Output(UInt(32.W))
  }
}

object LoadUseHazard {
  class DecodeChannel extends Bundle {
    val source = Output(Vec(2, UInt(5.W)))
    val hazard = Input(Bool())
  }
  class ExecuteChannel extends Bundle {
    val destination = Output(UInt(5.W))
    val isLoad = Output(Bool())
  }
}