package core.pipeline.stages

import chisel3._
import core.ControlTypes.{AluFunction, BitMaskerFunction, InstructionType, LeftOperand, MemoryAccessWidth, MemoryOperation, RightOperand, WriteBackSource, WriteSourceRegister}
import core.PipelineInterfaces.{DecodeToExecute, FetchToDecode}
import core.{Branching, Forwarding, Hazard, PipelineStage}
import core.pipeline.IntegerRegisterFile
import lib.Immediates.FromInstructionToImmediate
import lib.LookUp._
import lib.Opcode
import lib.util.{BoolVec, BundleItemAssignment}

class Decode extends PipelineStage(new FetchToDecode, new DecodeToExecute) {

  val io = IO(new Bundle {
    val registerSources = Input(new IntegerRegisterFile.SourceResponse)
    val branching = new Branching.DecodeChannel
    val hazardDetection = new Hazard.DecodeChannel
    val forwarding = new Forwarding.DecodeChannel
  })

  val immediate = lookUp(upstream.data.control.instructionType) in (
    InstructionType.I -> upstream.data.instruction.extractImmediate.iType,
    InstructionType.U -> upstream.data.instruction.extractImmediate.uType,
    InstructionType.S -> upstream.data.instruction.extractImmediate.sType,

  )
  val funct3 = upstream.data.instruction(14,12)
  val funct7 = Mux(upstream.data.control.isNotRegisterRegister, 0.U, upstream.data.instruction(31,15))
  val (opcode,_) = Opcode.safe(upstream.data.instruction(6,0))


  val operand = io.registerSources.data.zip(io.forwarding.channel).map { case (reg, channel) => Mux(channel.shouldForward, channel.value, reg)}

  val isCsrAccess = upstream.data.control.isSystem && funct3 =/= 0.U

  val comparison = VecInit(
    operand(0) === operand(1),
    operand(0) =/= operand(1),
    operand(0).asSInt < operand(1).asSInt,
    operand(0).asSInt >= operand(1).asSInt,
    operand(0) < operand(1),
    operand(0) >= operand(1)
  )
    .zip(upstream.data.compareSelect)
    .map { case (comp,en) => comp && en}
    .orR

  val branchDecision = upstream.data.control.isBranch && comparison

  io.branching.set(
    _.jump := upstream.data.control.isJalr,
    _.branch := upstream.data.control.isBranch,
    _.guess := upstream.data.control.guess,
    _.decision := branchDecision,
    _.target := Mux(upstream.data.control.isBranch, upstream.data.recoveryTarget, (operand(0).asSInt + upstream.data.instruction.extractImmediate.iType).asUInt),
    _.pc := upstream.data.pc
  )

  io.forwarding.nextExecuteInfo.set(
    _.canForward := upstream.data.control.hasRegisterWriteBack,
    _.destination := upstream.data.destination
  )

  io.hazardDetection.set(
    _.destination := upstream.data.destination,
    _.canForward := upstream.data.control.hasRegisterWriteBack,
    _.isLoad := upstream.data.control.isLoad
  )

  downstream.data.set(
    _.pc := upstream.data.pc,
    _.operand(0) := lookUp(upstream.data.control.leftOperand) in (LeftOperand.Register -> io.registerSources.data(0), LeftOperand.PC -> upstream.data.pc),
    _.operand(1) := Mux(upstream.data.control.add4, 4.U, lookUp(upstream.data.control.rightOperand) in (RightOperand.Register -> io.registerSources.data(1), RightOperand.Immediate -> immediate.asUInt)),
    _.csrIndex := immediate(11,0),
    _.writeValue := lookUp(upstream.data.control.writeSourceRegister) in (WriteSourceRegister.Left -> io.registerSources.data(0), WriteSourceRegister.Right -> io.registerSources.data(1)),
    _.source := upstream.data.source,
    _.destination := upstream.data.destination,
    _.funct3 := funct3,
    _.recoveryTarget := upstream.data.recoveryTarget,
    _.control.set(
      _.isBranch := upstream.data.control.isBranch,
      _.guess := upstream.data.control.guess,
      _.aluFunction := Mux(upstream.data.control.aluFunIsAdd, AluFunction.Addition, AluFunction.safe(funct7(5) ## funct3)._1),
      _.memoryOperation := MemoryOperation.safe(opcode.asUInt.apply(5))._1,
      _.withSideEffects.set(
        _.hasMemoryAccess := upstream.data.control.isLoad || upstream.data.control.isStore,
        _.isCsrWrite := isCsrAccess,
        _.isCsrRead := isCsrAccess && upstream.data.control.destinationIsNonZero,
        _.hasRegisterWriteBack := upstream.data.control.hasRegisterWriteBack
      )
    )
  )

  upstream.flowControl.set(
    _.stall := downstream.flowControl.stall || io.hazardDetection.hazard,
    _.flush := downstream.flowControl.flush || upstream.data.control.isJalr || (upstream.data.control.guess =/= branchDecision)
  )

  when(downstream.flowControl.flush || io.hazardDetection.hazard) {
    downstream.data.control.withSideEffects.set(
      _.hasMemoryAccess := 0.B,
      _.isCsrRead := 0.B,
      _.isCsrWrite := 0.B,
      _.hasRegisterWriteBack := 0.B
    )
  }


}

object DecodeEmitter extends App {
  emitVerilog(new Decode)
}