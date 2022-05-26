package cores.lib

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.Valid
import cores.lib.ControlAndStatusRegister.Interrupts
import lib.util.InputOutputExtender

object Exception {

  object Cause extends ChiselEnum {
    val None = Value(0.U)
    val LoadAddressMisaligned = Value(4.U)
    val LoadAccessFault = Value(5.U)
    val StoreAddressMisaligned = Value(6.U)
    val StoreAccessFault = Value(7.U)
    val EnvironmentCallFromMachineMode = Value(11.U)
    val MachineSoftwareInterrupt = Value(((1L << 31) | 3).U)
    val MachineTimerInterrupt = Value(((1L << 31) | 7).U)
    val MachineExternalInterrupt = Value(((1L << 31) | 11).U)
    val CustomInterrupt = Seq.tabulate(16)(i => ((1L << 31) | (16+i)).U)
  }
  implicit class CauseTypeExtender(x: Cause.Type) {
    def isInterrupt: Bool = x.asUInt.apply(31)
    def value: UInt = x.asUInt.apply(30,0)
  }

  class ExceptionBundle extends Bundle {
    val exception = Bool()
    val pc = UInt(32.W)
    val cause = Exception.Cause()
    val value = UInt(32.W)
  }
  class CSRChannel extends Bundle {
    val newException = Input(new ExceptionBundle)
    val interruptEnable = Output(new Interrupts)
    val interruptPending = Output(new Interrupts)
    val mepc = Output(UInt(32.W))
    val mtvec = Output(UInt(32.W))
    val updateMpie = Input(Bool())
    val resetMie = Input(Bool())
    val restoreMie = Input(Bool())
  }

}
