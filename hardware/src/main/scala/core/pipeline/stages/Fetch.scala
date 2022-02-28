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
  val isBranch = opcode === Opcode.branch
  // calculate jump or branch target
  val target = (upstream.data.pc.asSInt + Mux(jump, instruction.extractImmediate.jType, instruction.extractImmediate.bType)).asUInt

  val rightOperand = Mux(
    opcode.isOneOf(Opcode.jal,Opcode.jalr),
    RightOperand.Four,
    Mux(
      opcode === Opcode.register,
      RightOperand.Register,
      RightOperand.Immediate
    )
  )


  io.registerSources.index := source

  upstream.flowControl.set(
    _.stall := !io.instructionResponse.valid || downstream.flowControl.stall,
    _.flush := downstream.flowControl.flush
  )

  io.branching.set(
    _.jump := jump,
    _.takeGuess := isBranch,
    _.target := target,
    _.pc := upstream.data.pc
  )

  downstream.data.set(
    _.pc := upstream.data.pc,
    _.source := source,
    _.destination := destination,
    _.branchTarget := Mux(io.branching.guess, upstream.data.pc + 4.U, target), // pass on the fallback target, to recover a wrong branch prediction
    _.instruction := instruction,
    _.validOpcode := validOpcode,
    _.control.set(
      _.guess := io.branching.guess,
      _.isJalr := opcode === Opcode.jalr,
      _.isBranch := isBranch,
      _.destinationIsZero := destination === 0.U,
      _.writeSourceRegister := WriteSourceRegister(opcode =/= Opcode.system),
      _.leftOperand := LeftOperand(opcode.isOneOf(Opcode.auipc, Opcode.jal, Opcode.jalr)),
      _.rightOperand := rightOperand,
      _.instructionType := InstructionType.fromOpcode(opcode)
    )
  )

}









