package cores.nix

import chisel3._

object Branching {

  class ProgramCounterChannel extends Bundle {
    val target = Input(UInt(32.W))
    val jump = Input(Bool())
  }

}
