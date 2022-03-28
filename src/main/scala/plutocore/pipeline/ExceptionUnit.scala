package plutocore.pipeline

import chisel3._
import chisel3.experimental.ChiselEnum
import plutocore.pipeline.Exception.ExceptionBundle
import Exception.CauseTypeExtender
import lib.util.BundleItemAssignment
import plutocore.pipeline.ControlAndStatusRegister.Interrupts

object Exception {
  object Cause extends ChiselEnum {
    val None = Value(0.U)
    val LoadAddressMisaligned = Value(4.U)
    val LoadAccessFault = Value(5.U)
    val EnvironmentCallFromMachineMode = Value(11.U)
    val MachineSoftwareInterrupt = Value(((1L << 31) | 3).U)
    val MachineTimerInterrupt = Value(((1L << 31) | 7).U)
    val MachineExternalInterrupt = Value(((1L << 31) | 11).U)
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

  }
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
    val programCounter = Flipped(new Exception.ProgramCounterChannel)

  })

  io.csr.newException := io.decode
  io.programCounter.set(
    _.target := 0.U,
    _.jump := 0.B
  )


}
