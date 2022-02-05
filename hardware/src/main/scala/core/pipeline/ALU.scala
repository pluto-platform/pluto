package core.pipeline

import chisel3._
import core.AluFunction
import core.AluFunction._
import lib.LookUp._


class ALU extends Module {

  val io = IO(new Bundle {

    val operands = Input(Vec(2, UInt(32.W)))
    val result = Output(UInt(32.W))
    val operation = Input(AluFunction())

  })

  val uOp = io.operands
  val sOp = io.operands.map(_.asSInt)
  val shamt = uOp(1)(4,0)

  io.result := lookUp(io.operation) in (
    Subtraction          -> (sOp(0) - sOp(1)).asUInt,
    ShiftLeft            -> (uOp(0) << shamt),
    CompareSigned        -> (sOp(0) < sOp(1)).asUInt,
    CompareUnsinged      -> (uOp(0) < uOp(1)).asUInt,
    Xor                  -> (uOp(0) ^ uOp(1)),
    ShiftRight           -> (uOp(0) >> shamt),
    ShiftRightArithmetic -> (sOp(0) >> shamt).asUInt,
    Or                   -> (uOp(0) | uOp(1)),
    And                  -> (uOp(0) & uOp(1))
  ) orElse (sOp(0) + sOp(1)).asUInt

}
