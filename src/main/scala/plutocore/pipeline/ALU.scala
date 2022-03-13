package plutocore.pipeline

import chisel3._
import ControlTypes.AluFunction
import ControlTypes.AluFunction._
import lib.LookUp._


class ALU extends Module {

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
    CompareSigned        -> (sOp(0) < sOp(1)).asUInt,
    CompareUnsinged      -> (uOp(0) < uOp(1)).asUInt,
    Xor                  -> (uOp(0) ^ uOp(1)),
    ShiftRight           -> (uOp(0) >> shamt),
    ShiftRightArithmetic -> (sOp(0) >> shamt).asUInt,
    Or                   -> (uOp(0) | uOp(1)),
    And                  -> (uOp(0) & uOp(1))
  )

}
