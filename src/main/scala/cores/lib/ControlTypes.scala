package cores.lib

import chisel3.experimental.ChiselEnum
import chisel3.{Bundle, UInt}
import cores.lib.riscv.Opcode
import lib.LookUp.lookUp

object ControlTypes {


  // opcode(5)
  object MemoryOperation extends ChiselEnum {
    val Read, Write = Value

    def fromOpcode(opcode: Opcode.Type): MemoryOperation.Type = MemoryOperation.safe(opcode.asUInt.apply(5))._1
  }

  // funct3(1,0)
  object MemoryAccessWidth extends ChiselEnum {
    val Byte, HalfWord, Word = Value

    def fromFunct3(funct3: UInt): MemoryAccessWidth.Type = MemoryAccessWidth.safe(funct3(1, 0))._1
  }

  object MemoryAccessResult extends ChiselEnum {
    val Success, Denied, Failure = Value
  }



  class MemoryControl extends Bundle {
    val operation = MemoryOperation()
    val accessWidth = MemoryAccessWidth()
  }
}
