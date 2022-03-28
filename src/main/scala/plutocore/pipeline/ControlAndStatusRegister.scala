package plutocore.pipeline

import chisel3._
import chisel3.experimental.ChiselEnum

object ControlAndStatusRegister {
  object TrapMode extends ChiselEnum {
    val Direct = Value(0.U(2.W))
    val Vectored = Value(1.U(2.W))
  }
  class TrapVector extends Bundle {
    val base31_2 = UInt(30.W)
    val mode = TrapMode()
    def base: UInt = base31_2 ## 0.U(2.W)
  }
  class Interrupts extends Bundle {
    val global = Bool()
    val external = Bool()
    val timer = Bool()
    val software = Bool()
    val custom = Vec(16, Bool())
  }
  class PerformanceCounterControl extends Bundle {
    val cycle = Bool()
    val timer = Bool()
    val instructionsRetired = Bool()
  }

}
