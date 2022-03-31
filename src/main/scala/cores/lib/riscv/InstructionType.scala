package cores.lib.riscv

import chisel3.experimental.ChiselEnum
import lib.LookUp.lookUp

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
