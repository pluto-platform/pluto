package core.pipeline.stages

import chisel3._
import core.ControlTypes.{InstructionType, LeftOperand, RightOperand, WriteSourceRegister}
import core.Pipeline.InstructionChannel
import core.PipelineInterfaces._
import core.pipeline.IntegerRegisterFile
import core.{Branching, PipelineStage}
import lib.Immediates.FromInstructionToImmediate
import lib.LookUp._
import lib.Opcode
import lib.util.BundleItemAssignment



class Fetch extends PipelineStage(new ToFetch, new FetchToDecode) {

  val io = IO(new Bundle {
    val instructionResponse = Flipped(new InstructionChannel.Response)
    val branching = new Branching.FetchChannel
    val registerSources = Output(new IntegerRegisterFile.SourceRequest)
  })

  // insert NOP when flushing or when starved by the instruction cache
  val instruction = Mux(io.instructionResponse.valid || downstream.flowControl.flush, io.instructionResponse.bits.instruction, 0x13.U)

  val (opcode, validOpcode) = Opcode.safe(instruction(6,0))
  val source = VecInit(instruction(19,15), instruction(24,20))
  val destination = instruction(11,7)

  val jump = opcode === Opcode.jal
  val isJalr = opcode === Opcode.jalr
  val isBranch = opcode === Opcode.branch
  // calculate jump or branch target
  val branchOffset = Mux(jump, instruction.extractImmediate.jType, instruction.extractImmediate.bType)
  val target = (upstream.data.pc.asSInt + branchOffset).asUInt


  val isNotRegisterRegisterOperation = opcode =/= Opcode.register

  io.registerSources.index := source

  upstream.flowControl.set(
    _.stall := !io.instructionResponse.valid || downstream.flowControl.stall,
    _.flush := downstream.flowControl.flush
  )

  io.branching.set(
    _.jump := jump,
    _.takeGuess := isBranch,
    _.target := target,
    _.backwards := branchOffset < 0.S,
    _.pc := upstream.data.pc
  )

  downstream.data.set(
    _.pc := upstream.data.pc,
    _.source := source,
    _.destination := destination,
    _.branchRecoveryTarget := Mux(io.branching.guess, upstream.data.pc + 4.U, target), // pass on the fallback target, to recover a wrong branch prediction
    _.instruction := instruction,
    _.validOpcode := validOpcode,
    _.control.set(
      _.guess := io.branching.guess,
      _.isJalr := isJalr,
      _.isBranch := isBranch,
      _.aluFunIsAdd := isNotRegisterRegisterOperation && opcode =/= Opcode.immediate,
      _.add4 := jump || isJalr,
      _.destinationIsNonZero := destination =/= 0.U,
      _.writeSourceRegister := WriteSourceRegister(opcode =/= Opcode.system),
      _.leftOperand := LeftOperand(opcode === Opcode.auipc || jump || isJalr),
      _.rightOperand := RightOperand(isNotRegisterRegisterOperation),
      _.instructionType := InstructionType.fromOpcode(opcode)
    )
  )

}









