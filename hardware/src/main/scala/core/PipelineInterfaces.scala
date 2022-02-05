package core

import chisel3._
import chisel3.experimental.ChiselEnum
import core.pipeline.Instruction

// funct7(5) ## funct3
object AluFunction extends ChiselEnum {
  val Addition = Value(0x0.U)
  val ShiftLeft = Value(0x1.U)
  val CompareSigned = Value(0x2.U)
  val CompareUnsinged = Value(0x3.U)
  val Xor = Value(0x4.U)
  val ShiftRight = Value(0x5.U)
  val Or = Value(0x6.U)
  val And = Value(0x7.U)
  val Subtraction = Value(0x8.U)
  val ShiftRightArithmetic = Value(0xD.U)
}

object LeftOperand extends ChiselEnum {
  val Register, PC = Value
}
object RightOperand extends ChiselEnum {
  val Register, Immediate = Value
}

// (opcode === load/store) ## opcode(5)
object MemoryOperation extends ChiselEnum {
  val None = Value(0x0.U)
  val Read = Value(0x2.U)
  val Write = Value(0x3.U)
}

object MemoryAccessWidth extends ChiselEnum {
  val Byte, HalfWord, Word = Value
}

object WriteBackSource extends ChiselEnum {
  val AluResult, MemoryResult = Value
}

class AluControl extends Bundle {
  val aluFunction = AluFunction()
  val leftOperand = LeftOperand()
  val rightOperand = RightOperand()
}

class MemoryControl extends Bundle {
  val memoryOperation = MemoryOperation()
  val memoryAccessWidth = MemoryAccessWidth()
}

object PipelineInterfaces {

  class FetchToDecode extends Bundle {
    val pc = UInt(32.W)
    val instruction = UInt(32.W)
  }

  class DecodeToExecute extends Bundle {
    val pc = UInt(32.W)
    val RegisterOperand = Vec(2,UInt(32.W))
    val immediate = UInt(32.W)
    val registerSource = Vec(2,UInt(5.W))
    val registerDestination = UInt(5.W)

    val aluControl = new AluControl

    val memoryControl = new MemoryControl

    val writeBackSource = WriteBackSource()
  }

  class ExecuteToMemory extends Bundle {
    val pc = UInt(32.W)
    val AluResult = UInt(32.W)
    val writeData = UInt(32.W)
    val registerDestination = UInt(5.W)

    val memoryControl = new MemoryControl

    val writeBackSource = WriteBackSource()
  }

  class MemoryToWriteBack extends Bundle {
    val pc = UInt(32.W)
    val AluResult = UInt(32.W)
    val writeData = UInt(32.W)
    val registerDestination = UInt(5.W)

    val writeBackSource = WriteBackSource()
  }

}

abstract class PipelineStage[IN <: Data, OUT <: Data](inGen: => IN, outGen: => OUT) extends MultiIOModule {
  val in = IO(Input(inGen))
  val out = IO(Output(outGen))

  val control = IO(new Bundle {

  })


  def :>[S <: PipelineStage[OUT,_]](that: S): S = {
    RegNext(out) <> that.in
    that
  }
}
