package core.pipeline.stages

import chisel3._
import core.ControlTypes.{AluFunction, BitMaskerFunction, InstructionType, LeftOperand, MemoryAccessWidth, MemoryOperation, RightOperand, WriteBackSource, WriteSourceRegister}
import core.PipelineInterfaces.{DecodeToExecute, FetchToDecode}
import core.{Branching, PipelineStage}
import core.pipeline.IntegerRegisterFile
import lib.Immediates.FromInstructionToImmediate
import lib.LookUp._
import lib.Opcode
import lib.util.BundleItemAssignment

class Decode extends PipelineStage(new FetchToDecode, new DecodeToExecute) {

  val io = IO(new Bundle {
    val registerSources = Input(new IntegerRegisterFile.SourceResponse)
    val branching = Output(new Branching.DecodeChannel)
  })

  val immediate = lookUp(upstream.data.control.instructionType) in (
    InstructionType.I -> upstream.data.instruction.extractImmediate.iType,
    InstructionType.U -> upstream.data.instruction.extractImmediate.uType,
    InstructionType.S -> upstream.data.instruction.extractImmediate.sType
  )
  val funct3 = upstream.data.instruction(14,12)
  val funct7 = upstream.data.instruction(31,15)
  val (opcode,_) = Opcode.safe(upstream.data.instruction(6,0))
  val op = io.registerSources.data
  val comparisons = VecInit(
    op(0) === op(1),
    op(0) =/= op(1),
    op(0).asSInt < op(1).asSInt,
    op(0).asSInt >= op(1).asSInt,
    op(0) < op(1),
    op(0) >= op(1)
  )
  val jump = upstream.data.control.isJump || (upstream.data.control.isBranch && comparisons(funct3))
  val isLoad = opcode === Opcode.load
  val isStore = opcode === Opcode.store
  val isCsrAccess = opcode === Opcode.system && funct3 =/= 0.U

  io.branching.set(
    _.jump := jump,
    _.target := upstream.data.branchTarget
  )

  downstream.data.set(
    _.pc := upstream.data.pc,
    _.operand(0) := lookUp(upstream.data.control.leftOperand) in (LeftOperand.Register -> io.registerSources.data(0), LeftOperand.PC -> upstream.data.pc),
    _.operand(1) := lookUp(upstream.data.control.rightOperand) in (RightOperand.Register -> io.registerSources.data(1), RightOperand.Immediate -> immediate.asUInt),
    _.csrIndex := upstream.data.instruction.extractImmediate.iType.apply(11,0),
    _.writeValue := lookUp(upstream.data.control.writeSourceRegister) in (WriteSourceRegister.Left -> io.registerSources.data(0), WriteSourceRegister.Right -> io.registerSources.data(1)),
    _.source := upstream.data.source,
    _.destination := upstream.data.destination,
    _.funct3 := funct3,
    _.funct7_5 := funct7(5),
    _.control.set(
      _.allowForwarding(0) := upstream.data.control.leftOperand === LeftOperand.Register,
      _.allowForwarding(1) := upstream.data.control.rightOperand === RightOperand.Register,
      _.isLoad := isLoad,
      _.memoryOperation := MemoryOperation.safe(opcode.asUInt.apply(5))._1,
      _.withSideEffects.set(
        _.hasMemoryAccess := isLoad || isStore,
        _.isCsrWrite := isCsrAccess,
        _.isCsrRead := isCsrAccess && !upstream.data.control.destinationIsZero,
        _.hasRegisterWriteBack := !opcode.isOneOf(Opcode.store, Opcode.branch)
      )
    )
  )

  when(downstream.flowControl.flush) {
    downstream.data.control.withSideEffects.set(
      _.hasMemoryAccess := 0.B,
      _.isCsrRead := 0.B,
      _.isCsrWrite := 0.B,
      _.hasRegisterWriteBack := 0.B
    )
  }


}