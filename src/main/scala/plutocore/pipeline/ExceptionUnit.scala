package plutocore.pipeline

import chisel3._
import chisel3.experimental.ChiselEnum

object Exception {
  object Cause extends ChiselEnum {
    val LoadAddressMisaligned = Value(4.U)
    val LoadAccessFault = Value(5.U)
    val EnvironmentCallFromMachineMode = Value(11.U)
    val MachineSoftwareInterrupt = Value(((1 << 31) | 3).U)
    val MachineTimerInterrupt = Value(((1 << 31) | 7).U)
    val MachineExternalInterrupt = Value(((1 << 31) | 11).U)
  }
  implicit class CauseTypeExtender(x: Cause.Type) {
    def isInterrupt: Bool = x.asUInt.apply(31)
  }
  class ExceptionBundle extends Bundle {
    val exception = Bool()
    val pc = UInt(32.W)
    val cause = Exception.Cause()
    val value = UInt(32.W)
  }
  class CSRChannel extends Bundle {
    val newException = Output(new ExceptionBundle)
    val globalInterruptEnable = Input(Bool())
    val interruptEnable = Input(Vec(32, Bool()))

  }
}

class ExceptionUnit extends Module {



}
