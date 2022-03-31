package cores.nix


import chisel3.experimental.ChiselEnum
import chisel3.{Bundle, UInt}
import cores.lib.riscv.Opcode
import lib.LookUp.lookUp

object ControlTypes {

  object LeftOperand extends ChiselEnum {
    val Register, PC, Zero = Value
  }

  object RightOperand extends ChiselEnum {
    val Register, Immediate, Four = Value
  }

  object WriteBackSource extends ChiselEnum {
    val AluResult, MemoryResult, CSRResult = Value
  }
}
