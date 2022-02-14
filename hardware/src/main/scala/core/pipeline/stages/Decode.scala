package core.pipeline.stages

import chisel3._
import core.ControlTypes.{InstructionType, LeftOperand, RightOperand}
import core.PipelineInterfaces.{DecodeToExecute, FetchToDecode}
import core.PipelineStage
import core.pipeline.IntegerRegisterFile
import lib.Immediates.FromInstructionToImmediate
import lib.LookUp._
import lib.util.BundleItemAssignment

class Decode extends PipelineStage(new FetchToDecode, new DecodeToExecute) {

  val io = IO(new Bundle {
    val registerSources = Input(new IntegerRegisterFile.SourceResponse)
  })


  val immediate = lookUp(upstream.control.instructionType) in (
    InstructionType.I -> upstream.instruction.extractImmediate.iType,
    InstructionType.U -> upstream.instruction.extractImmediate.uType,
    InstructionType.S -> upstream.instruction.extractImmediate.sType
  )


  downstream.set(
    _.operand(0) := lookUp(upstream.control.leftOperand) in (LeftOperand.Register -> io.registerSources.data(0), LeftOperand.PC -> upstream.pc),
    _.operand(1) := lookUp(upstream.control.rightOperand) in (RightOperand.Register -> io.registerSources.data(1), RightOperand.Immediate -> immediate),
    _.immediate := immediate
  )


}