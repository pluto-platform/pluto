package core.pipeline

import chisel3._
import lib.Opcode

class Instruction extends Bundle {
  val opcode = Opcode()
  val rs = Vec(2, UInt(5.W))
  val rd = UInt(5.W)
  val funct3 = UInt(3.W)
  val funct7 = UInt(7.W)
  val imm = UInt(32.W)
  val invalid = Bool()
}

object Instruction {

  implicit class UIntToInstruction(word: UInt) {
    def toInstruction: Instruction = {
      val i = Wire(new Instruction)
      val (opcode,valid) = Opcode.safe(word(6,0))
      i.opcode := opcode
      i.invalid := !valid
      i.rd := word(11,7)
      i.rs(0) := word(19,15)
      i.rs(1) := word(24,20)
      i.funct3 := word(14,12)
      i.funct7 := word(31,25)
      i
    }
  }

}

class Decoder extends Module {



}
