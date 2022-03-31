package cores.nix

import chisel3._
import cores.lib.Exception
import cores.lib.Exception.ExceptionBundle
import lib.util.BundleItemAssignment

object ExceptionUnit {
  class ProgramCounterChannel extends Bundle {
    val target = Input(UInt(32.W))
    val jump = Input(Bool())
  }
  class InterruptChannel extends Bundle {

  }
}

class ExceptionUnit extends Module {

  val io = IO(new Bundle {

    val decode = Input(new ExceptionBundle)
    val writeBack = Input(new ExceptionBundle)
    val csr = Flipped(new Exception.CSRChannel)
    val programCounter = Flipped(new ExceptionUnit.ProgramCounterChannel)

  })

  io.csr.newException := io.decode
  io.programCounter.set(
    _.target := 0.U,
    _.jump := 0.B
  )


}
