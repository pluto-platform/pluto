package plutocore.pipeline

import chisel3.experimental.ChiselEnum
import chisel3._
import lib.LookUp.lookUp
import plutocore.lib.Opcode

object ControlTypes {
  // funct7(5) ## funct3 when Opcode.register
  // funct3 when Opcode.Immediate
  object AluFunction extends ChiselEnum {
    val Addition = Value(0x0.U)
    val ShiftLeft = Value(0x1.U)
    val LessThanSigned = Value(0x2.U)
    val LessThanUnsigned = Value(0x3.U)
    val Xor = Value(0x4.U)
    val ShiftRight = Value(0x5.U)
    val Or = Value(0x6.U)
    val And = Value(0x7.U)
    val Subtraction = Value(0x8.U)
    val ShiftRightArithmetic = Value(0xD.U)
  }

  object LeftOperand extends ChiselEnum {
    val Register, PC, Zero = Value
  }

  object RightOperand extends ChiselEnum {
    val Register, Immediate, Four = Value
  }

  object InstructionType extends ChiselEnum {
    val R, I, S, B, U, J = Value

    def fromOpcode(opcode: Opcode.Type): InstructionType.Type = {
      lookUp(opcode) in(
        Opcode.load -> I,
        Opcode.miscMem -> I,
        Opcode.immediate -> I,
        Opcode.auipc -> U,
        Opcode.store -> S,
        Opcode.register -> R,
        Opcode.lui -> U,
        Opcode.branch -> B,
        Opcode.jalr -> I,
        Opcode.jal -> J,
        Opcode.system -> I
      )
    }
  }

  // opcode(5)
  object MemoryOperation extends ChiselEnum {
    val Read, Write = Value

    def fromOpcode(opcode: Opcode.Type): MemoryOperation.Type = MemoryOperation(opcode.asUInt.apply(5))
  }

  // funct3(1,0)
  object MemoryAccessWidth extends ChiselEnum {
    val Byte, HalfWord, Word = Value

    def fromFunct3(funct3: UInt): MemoryAccessWidth.Type = MemoryAccessWidth(funct3(1, 0))
  }

  object MemoryAccessResult extends ChiselEnum {
    val Success, Denied, Failure = Value
  }

  object WriteBackSource extends ChiselEnum {
    val AluResult, MemoryResult, CSRResult = Value
  }

  // funct3(1,0)
  object BitMaskerFunction extends ChiselEnum {
    val PassThrough = Value(1.U)
    val Set = Value(2.U)
    val Clear = Value(3.U)
  }

  class MemoryControl extends Bundle {
    val memoryOperation = MemoryOperation()
    val memoryAccessWidth = MemoryAccessWidth()
  }
}
