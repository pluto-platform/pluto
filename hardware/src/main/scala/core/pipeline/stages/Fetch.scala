package core.pipeline.stages

import chisel3._
import core.ControlTypes.{InstructionType, LeftOperand, RightOperand, WriteSourceRegister}
import core.Pipeline.InstructionChannel
import core.PipelineInterfaces._
import core.pipeline.IntegerRegisterFile
import core.{Branching, Forwarding, Hazard, PipelineStage}
import lib.Immediates.FromInstructionToImmediate
import lib.LookUp._
import lib.Opcode
import lib.util.BundleItemAssignment



class Fetch extends PipelineStage(new ToFetch, new FetchToDecode) {

  val io = IO(new Bundle {
    val instructionResponse = Flipped(new InstructionChannel.Response)
    val branching = new Branching.FetchChannel
    val registerSources = Output(new IntegerRegisterFile.SourceRequest)
    val forwarding = new Forwarding.FetchChannel
    val hazard = new Hazard.FetchChannel
  })


  val instruction = io.instructionResponse.bits.instruction

  val (opcode, validOpcode) = Opcode.safe(instruction(6,0))
  val source = VecInit(instruction(19,15), instruction(24,20))
  val destination = instruction(11,7)

  val jump = opcode === Opcode.jal
  val isJalr = opcode === Opcode.jalr
  val isBranch = opcode === Opcode.branch
  val isLoad = opcode === Opcode.load
  val isStore = opcode === Opcode.store
  val isImmediate = opcode === Opcode.immediate
  val isSystem = opcode === Opcode.system
  // calculate jump or branch target
  // TODO: can we use a single bit here?
  val branchImmediate = instruction.extractImmediate.bType
  val offset = Mux(instruction(3), instruction.extractImmediate.jType, branchImmediate)
  val target = (upstream.data.pc.asSInt + offset).asUInt
  val nextPc = upstream.data.pc + 4.U


  val isNotRegisterRegisterOperation = opcode =/= Opcode.register
  val destinationIsNoneZero = destination =/= 0.U

  val leftOperandIsNotRegister = opcode === Opcode.auipc || jump || isJalr
  val rightOperandIsNotRegister = isNotRegisterRegisterOperation && !isBranch

  io.registerSources.index := source

  upstream.flowControl.set(
    _.stall := !io.instructionResponse.valid || downstream.flowControl.stall,
    _.flush := downstream.flowControl.flush
  )

  io.branching.set(
    _.jump := jump,
    _.takeGuess := isBranch,
    _.target := target,
    _.backwards := branchImmediate < 0.S,
    _.pc := upstream.data.pc,
    _.nextPc := nextPc
  )

  io.forwarding.set(
    _.source(0).id := source(0),
    _.source(0).neededInDecode := isBranch || isJalr,
    _.source(0).acceptsForwarding := !leftOperandIsNotRegister,
    _.source(1).id := source(1),
    _.source(1).neededInDecode := isBranch,
    _.source(1).acceptsForwarding := !rightOperandIsNotRegister
  )

  io.hazard.set(
    _.source := source,
    _.isBranch := isBranch,
    _.isJalr := isJalr
  )

  downstream.data.set(
    _.pc := upstream.data.pc,
    _.source := source,
    _.destination := destination,
    _.recoveryTarget := Mux(io.branching.guess, nextPc, target), // pass on the fallback target, to recover a wrong branch prediction
    _.instruction := instruction,
    _.validOpcode := validOpcode,
    _.control.set(
      _.guess := io.branching.guess,
      _.isJalr := isJalr,
      _.isBranch := isBranch,
      _.isLoad := isLoad,
      _.isStore := isStore,
      _.isImmediate := isImmediate,
      _.isSystem := isSystem,
      _.aluFunIsAdd := isNotRegisterRegisterOperation && isImmediate,
      _.add4 := jump || isJalr,
      _.isNotRegisterRegister := isNotRegisterRegisterOperation,
      _.destinationIsNonZero := destinationIsNoneZero,
      _.hasRegisterWriteBack := (!isStore && !isBranch) && destinationIsNoneZero,
      _.writeSourceRegister := WriteSourceRegister(!isSystem),
      _.leftOperand := LeftOperand(leftOperandIsNotRegister),
      _.rightOperand := RightOperand(rightOperandIsNotRegister),
      _.instructionType := InstructionType.fromOpcode(opcode)
    )
  )
  // insert NOP when flushing or when starved by the instruction cache
  when(downstream.flowControl.flush || !io.instructionResponse.valid) {
    downstream.data.set(
      //_.destination := 0.U, maybe?
      _.instruction := 0x13.U,
      _.validOpcode := 1.B,
      _.control.set(
        _.isJalr := 0.B,
        _.isBranch := 0.B,
        _.isLoad := 0.B,
        _.hasRegisterWriteBack := 0.B
      )
    )
  }

}









