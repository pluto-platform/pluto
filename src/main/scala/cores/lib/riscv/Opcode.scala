package cores.lib.riscv

import chisel3._
import chisel3.experimental.ChiselEnum

object Opcode extends ChiselEnum {
  val load = Value(0x03.U) // I-type | 0x03
  val miscMem = Value(0x0F.U) // I-type | 0x0F
  val immediate = Value(0x13.U) // I-type | 0x13
  val auipc = Value(0x17.U) // U-type | 0x17
  val store = Value(0x23.U) // S-type | 0x23
  val register = Value(0x33.U) // R-type | 0x33
  val lui = Value(0x37.U) // U-type | 0x37
  val branch = Value(0x63.U) // B-type | 0x63
  val jalr = Value(0x67.U) // I-type | 0x67
  val jal = Value(0x6F.U) // J-Type | 0x6F
  val system = Value(0x73.U) // I-type | 0x73

  def fromInstruction(instr: UInt): Opcode.Type = Opcode.safe(instr(6, 0))._1
  def safeFromInstruction(instr: UInt): (Opcode.Type, Bool) = Opcode.safe(instr(6, 0))
}
