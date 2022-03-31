package cores.modules

import chisel3._
import chisel3.experimental.ChiselEnum
import cores.lib.riscv.Opcode
import lib.LookUp._


object ALU {
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

    def fromInstruction(instr: UInt): AluFunction.Type = {
      val opcode = Opcode.fromInstruction(instr)
      val funct7_5 = Mux(opcode === Opcode.register, instr(30), 0.B)
      val funct3 = Mux(opcode.isOneOf(Opcode.register, Opcode.immediate), instr(14,12), 0.U)
      AluFunction.safe(funct7_5 ## funct3)._1
    }
  }
}

class ALU extends Module {
  import ALU.AluFunction
  import ALU.AluFunction._

  val io = IO(new Bundle {

    val operand = Input(Vec(2, UInt(32.W)))
    val result = Output(UInt(32.W))
    val operation = Input(AluFunction())

  })

  val uOp = io.operand
  val sOp = io.operand.map(_.asSInt)
  val shamt = uOp(1)(4,0)

  io.result := lookUp(io.operation) in (
    Addition             -> (sOp(0) + sOp(1)).asUInt,
    Subtraction          -> (sOp(0) - sOp(1)).asUInt,
    ShiftLeft            -> (uOp(0) << shamt),
    Xor                  -> (uOp(0) ^ uOp(1)),
    ShiftRight           -> (uOp(0) >> shamt),
    ShiftRightArithmetic -> (sOp(0) >> shamt).asUInt,
    Or                   -> (uOp(0) | uOp(1)),
    And                  -> (uOp(0) & uOp(1)),
    LessThanSigned       -> (sOp(0) < sOp(1)).asUInt,
    LessThanUnsigned     -> (uOp(0) < uOp(1)).asUInt
  )

}
