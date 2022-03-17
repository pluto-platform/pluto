package plutocore.pipeline

import chisel3._
import chisel3.experimental.ChiselEnum
import plutocore.pipeline.Exception.ExceptionBundle
import Exception.CauseTypeExtender

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
    val globalInterruptEnable = Output(Bool())
    val interruptEnable = Output(Vec(32, Bool()))

  }
  class ProgramCounterChannel extends Bundle {
    val target = Input(UInt(32.W))
    val jump = Input(Bool())
  }
  class InterruptChannel extends Bundle {
    val
  }
}

class ExceptionUnit extends Module {

  val io = IO(new Bundle {

    val decode = Input(new ExceptionBundle)
    val writeBack = Input(new ExceptionBundle)
    val csr = Flipped(new Exception.CSRChannel)
    val programCounter = Flipped(new Exception.ProgramCounterChannel)

  })

  val cause = Mux(io.writeBack.exception, io.writeBack.cause, io.decode.cause)

  io.csr.interruptEnable(cause.value)



}
