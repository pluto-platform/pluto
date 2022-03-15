package plutocore.pipeline.stages

import chisel3._
import plutocore.pipeline.ControlTypes.{AluFunction, InstructionType, LeftOperand, MemoryOperation, RightOperand}
import plutocore.pipeline.PipelineInterfaces.{DecodeToExecute, FetchToDecode}
import plutocore.pipeline.{Branching, Forwarding, Hazard, IntegerRegisterFile, PipelineStage}
import plutocore.lib.Immediates.FromInstructionToImmediate
import lib.LookUp._
import lib.util.{BoolVec, BundleItemAssignment, SeqToVecMethods}
import plutocore.lib.Opcode

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
  val source = VecInit(upstream.data.instruction(19,15), upstream.data.instruction(24,20))
  val destination = upstream.data.instruction(11,7)
  val funct3 = upstream.data.instruction(14,12)
  val funct7 = Mux(!upstream.data.control.isRegister, 0.U, upstream.data.instruction(31,15))
  val (opcode,_) = Opcode.safe(upstream.data.instruction(6,0))


  io.forwarding.channel zip source foreach { case (channel, source) => channel.source := source}
  val operand = io.registerSources.data.zip(io.forwarding.channel).map { case (reg, channel) => Mux(channel.shouldForward, channel.value, reg) }.toVec

  val aluOperand = VecInit(
    lookUp(upstream.data.control.leftOperand) in (
      LeftOperand.Register -> operand(0),
      LeftOperand.PC -> upstream.data.pc,
      LeftOperand.Zero -> 0.U
    ),
    lookUp(upstream.data.control.rightOperand) in (
      RightOperand.Register -> operand(1),
      RightOperand.Immediate -> immediate.asUInt,
      RightOperand.Four -> 4.U
    )
  )

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



  io.hazardDetection.set(
    _.source := source,
    _.isJalr := upstream.data.control.isJalr,
    _.isBranch := upstream.data.control.isBranch
  )

  downstream.data.set(
    _.pc := upstream.data.pc,
    _.operand := aluOperand,
    _.csrIndex := immediate(11,0),
    _.writeValue := operand(upstream.data.control.writeSourceRegister),
    _.source := source,
    _.destination := destination,
    _.funct3 := funct3,
    _.control.set(
      _.isLoad := upstream.data.control.isLoad,
      _.acceptsForwarding(0) := upstream.data.control.leftOperand === LeftOperand.Register,
      _.acceptsForwarding(1) := upstream.data.control.rightOperand === RightOperand.Register,
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