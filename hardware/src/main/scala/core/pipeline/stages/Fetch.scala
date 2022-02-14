package core.pipeline.stages

import chisel3._
import core.ControlTypes.{InstructionType, LeftOperand, RightOperand, WriteSourceRegister}
import core.Pipeline.InstructionChannel
import core.PipelineInterfaces._
import core.pipeline.IntegerRegisterFile
import core.{Branching, PipelineStage}
import lib.Immediates.FromInstructionToImmediate
import lib.Opcode
import lib.util.BundleItemAssignment



class Fetch extends PipelineStage(new ToFetch, new FetchToDecode) {

  val io = IO(new Bundle {
    val instructionResponse = Flipped(new InstructionChannel.Response)
    val branching = new Branching.FetchChannel
    val registerSources = Output(new IntegerRegisterFile.SourceRequest)
  })


  val instruction = Mux(io.instructionResponse.valid, io.instructionResponse.bits.instruction, 0x13.U)
  val (opcode, validOpcode) = Opcode.safe(instruction(6,0))
  val branchOffset = Mux(opcode === Opcode.jal, instruction.extractImmediate.jType, instruction.extractImmediate.bType)
  val source = VecInit(instruction(19,15), instruction(24,20))
  val destination = instruction(11,7)


  io.registerSources.index := source

  io.branching.set(
    _.jump := opcode === Opcode.jal,
    _.takeGuess := opcode === Opcode.branch,
    _.pc := upstream.pc,
    _.offset := branchOffset
  )

  downstream.set(
    _.pc := upstream.pc,
    _.source := source,
    _.destination := destination,
    _.branchOffset := branchOffset,
    _.instruction := instruction,
    _.validOpcode := validOpcode,
    _.control.set(
      _.branchWasTaken := io.branching.isTakingBranch,
      _.writeSourceRegister := WriteSourceRegister(opcode =/= Opcode.system),
      _.leftOperand := LeftOperand(opcode === Opcode.auipc),
      _.rightOperand := RightOperand(opcode =/= Opcode.branch && opcode =/= Opcode.register),
      _.instructionType := InstructionType.fromOpcode(opcode)
    )
  )

}









