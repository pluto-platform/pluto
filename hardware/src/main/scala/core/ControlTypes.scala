package core

import chisel3._
import chisel3.experimental.ChiselEnum

object ControlTypes {
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
  object WriteSourceRegister extends ChiselEnum {
    val Left, Right = Value
  }
  object InstructionType extends ChiselEnum {
    val R, I, S, B, U, J = Value
  }

  // opcode(5)
  object MemoryOperation extends ChiselEnum {
    val Read, Write = Value
  }

  // funct3(1,0)
  object MemoryAccessWidth extends ChiselEnum {
    val Byte, HalfWord, Word = Value
  }

  object MemoryAccessResult extends ChiselEnum {
    val Success, Denied, Failure = Value
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
}
