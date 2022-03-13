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
  class ForwardingChannel extends Bundle {
    val source = Output(UInt(5.W))
    val shouldForward = Input(Bool())
    val value = Input(UInt(32.W))
  }

  class DecodeChannel extends Bundle {
    val channel = Vec(2, new ForwardingChannel)
  }
  class ExecuteChannel extends Bundle {
    val channel = Vec(2, new ForwardingChannel)
  }
  class MemoryChannel extends Bundle {
    val destination = Output(UInt(5.W))
    val canForward = Output(Bool())
    val value = Output(UInt(32.W))
  }
  class WriteBackChannel extends Bundle {
    val destination = Output(UInt(5.W))
    val canForward = Output(Bool())
    val value = Output(UInt(32.W))
  }
}
object OldForwarding {
  class InfoProvider extends Bundle {
    val destination = UInt(5.W)
    val canForward = Bool()
  }
  class ForwardingChannel extends Bundle {
    val shouldForward = Bool()
    val value = UInt(32.W)
  }
  class SourceBundle extends Bundle {
    val id = UInt(5.W)
    val neededInDecode = Bool()
    val acceptsForwardingInExecute = Bool()
  }

  class FetchChannel extends Bundle {
    val source = Output(Vec(2, new SourceBundle))
  }
  class DecodeChannel extends Bundle {
    val nextExecuteInfo = Output(new InfoProvider)

    val channel = Input(Vec(2, new ForwardingChannel))
  }
  class ExecuteChannel extends Bundle {
    val nextMemoryInfo = Output(new InfoProvider)

    val channel = Input(Vec(2, new ForwardingChannel))
  }
  class MemoryChannel extends Bundle {
    val nextWriteBackInfo = Output(new InfoProvider)
    val writeBackValue = Output(UInt(32.W))
  }
  class WriteBackChannel extends Bundle {
    val writeBackValue = Output(UInt(32.W))
  }
}

object Hazard {
  class DecodeChannel extends Bundle {
    val source = Output(Vec(2, UInt(5.W)))
    val isBranch = Output(Bool())
    val isJalr = Output(Bool())

    val hazard = Input(Bool())
  }
  class ExecuteChannel extends Bundle {
    val destination = Output(UInt(5.W))
    val isLoad = Output(Bool())
    val canForward = Output(Bool())
  }
  class MemoryChannel extends Bundle {
    val destination = Output(UInt(5.W))
    val isLoad = Output(Bool())
    val canForward = Output(Bool())
  }
}