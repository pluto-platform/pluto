package core.pipeline.stages

import chisel3._
import core.ControlTypes.{AluFunction, InstructionType, LeftOperand, RightOperand, WriteSourceRegister}
import core.PipelineInterfaces.{DecodeToExecute, FetchToDecode}
import core.{Branching, PipelineStage}
import core.pipeline.IntegerRegisterFile
import lib.Immediates.FromInstructionToImmediate
import lib.LookUp._
import lib.util.BundleItemAssignment

class Decode extends PipelineStage(new FetchToDecode, new DecodeToExecute) {

  val io = IO(new Bundle {
    val registerSources = Input(new IntegerRegisterFile.SourceResponse)
    val branching = Output(new Branching.DecodeChannel)
  })

  val immediate = lookUp(upstream.control.instructionType) in (
    InstructionType.I -> upstream.instruction.extractImmediate.iType,
    InstructionType.U -> upstream.instruction.extractImmediate.uType,
    InstructionType.S -> upstream.instruction.extractImmediate.sType
  )
  val funct3 = upstream.instruction(14,12)
  val op = io.registerSources.data
  val comparisons = VecInit(
    op(0) === op(1),
    op(0) =/= op(1),
    op(0).asSInt < op(1).asSInt,
    op(0).asSInt >= op(1).asSInt,
    op(0) < op(1),
    op(0) >= op(1)
  )
  val jump = upstream.control.isJump || (upstream.control.isBranch && comparisons(funct3))

  io.branching.set(
    _.jump := jump,
    _.target := upstream.branchTarget
  )

  downstream.set(
    _.operand(0) := lookUp(upstream.control.leftOperand) in (LeftOperand.Register -> io.registerSources.data(0), LeftOperand.PC -> upstream.pc),
    _.operand(1) := lookUp(upstream.control.rightOperand) in (RightOperand.Register -> io.registerSources.data(1), RightOperand.Immediate -> immediate),
    _.immediate := immediate,
    _.writeValue := lookUp(upstream.control.writeSourceRegister) in (WriteSourceRegister.Left -> io.registerSources.data(0), WriteSourceRegister.Right -> io.registerSources.data(1)),

  )

  when(control.downstream.flush) {
    downstream.set(
      _.destination := 0.U,
      _.control.set(
        _.isLoad := 0.B,
        _.aluFunction := AluFunction.
      )
    )
  }


}