package plutocore.pipeline.stages

import chisel3._
import chisel3.util.UIntToOH
import plutocore.pipeline.ControlTypes.{InstructionType, LeftOperand, RightOperand}
import plutocore.pipeline.Pipeline.InstructionChannel
import plutocore.pipeline.PipelineInterfaces._
import plutocore.pipeline.{Branching, IntegerRegisterFile, PipelineStage}
import plutocore.lib.Immediates.FromInstructionToImmediate
import lib.util.BundleItemAssignment
import plutocore.branchpredictor.BranchPrediction
import plutocore.lib.Opcode



class Fetch extends PipelineStage(new ToFetch, new FetchToDecode) {

  val io = IO(new Bundle {
    val instructionResponse = Flipped(new InstructionChannel.Response)
    val branching = new Branching.FetchChannel
    val registerSources = Output(new IntegerRegisterFile.SourceRequest)
  })

  val instruction = io.instructionResponse.bits.instruction

  val (opcode, validOpcode) = Opcode.safe(instruction(6,0))
  val source = VecInit(instruction(19,15), instruction(24,20))
  val destination = instruction(11,7)
  val funct3 = instruction(14,12)

  val isJump = opcode === Opcode.jal
  val isJalr = opcode === Opcode.jalr
  val isBranch = opcode === Opcode.branch
  val isLoad = opcode === Opcode.load
  val isStore = opcode === Opcode.store
  val isLui = opcode === Opcode.lui
  val isAuipc = opcode === Opcode.auipc
  val isImmediate = opcode === Opcode.immediate
  val isSystem = opcode === Opcode.system
  val isRegister = opcode === Opcode.register
  val destinationIsNoneZero = destination =/= 0.U

  val leftOperand = Mux(isAuipc || isJalr || isJump, LeftOperand.PC, Mux(isLui, LeftOperand.Zero, LeftOperand.Register))
  val rightOperand = Mux(isRegister, RightOperand.Register, Mux(isJalr || isJump, RightOperand.Four, RightOperand.Immediate))

  // calculate jump or branch target
  val branchImmediate = instruction.extractImmediate.bType
  val offset = Mux(instruction(3), instruction.extractImmediate.jType, branchImmediate) // bit three makes jal and branches distinct
  val target = (upstream.data.pc.asSInt + offset).asUInt
  val nextPc = upstream.data.pc + 4.U


  io.registerSources.index := source

  upstream.flowControl.set(
    _.stall := !io.instructionResponse.valid || downstream.flowControl.stall,
    _.flush := downstream.flowControl.flush
  )

  io.branching.set(
    _.jump := isJump,
    _.takeGuess := isBranch,
    _.target := target,
    _.backwards := branchImmediate < 0.S,
    _.pc := upstream.data.pc,
    _.nextPc := nextPc
  )



  downstream.data.set(
    _.pc := upstream.data.pc,
    _.nextPc := nextPc,
    _.recoveryTarget := Mux(io.branching.guess, nextPc, target), // pass on the fallback target, to recover a wrong branch prediction
    _.instruction := instruction,
    _.validOpcode := validOpcode,
    _.compareSelect := UIntToOH(funct3).asBools.take(6),
    _.control.set(
      _.guess := io.branching.guess,
      _.isJalr := isJalr,
      _.isBranch := isBranch,
      _.isLoad := isLoad,
      _.isStore := isStore,
      _.isLui := isLui,
      _.isImmediate := isImmediate,
      _.isSystem := isSystem,
      _.isRegister := isRegister,
      _.aluFunIsAdd := !isRegister && !isImmediate,
      _.destinationIsNonZero := destinationIsNoneZero,
      _.hasRegisterWriteBack := (!isStore && !isBranch) && destinationIsNoneZero,
      _.writeSourceRegister := (!isSystem).asUInt,
      _.leftOperand := leftOperand,
      _.rightOperand := rightOperand,
      _.instructionType := InstructionType.fromOpcode(opcode)
    )
  )
  // insert NOP when flushing or when starved by the instruction cache
  when(downstream.flowControl.flush || !io.instructionResponse.valid) {
    downstream.data.set(
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









