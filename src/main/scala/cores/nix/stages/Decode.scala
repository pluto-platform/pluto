package cores.nix.stages

import chisel3._
import lib.LookUp._
import lib.util.BundleItemAssignment
import cores.lib.ControlTypes._
import cores.nix.Hazard
import cores.lib.Exception
import Interfaces.{DecodeToExecute, FetchToDecode}
import chisel3.util.{MuxCase, MuxLookup, UIntToOH}
import cores.lib.Exception.ExceptionBundle
import cores.lib.riscv.Immediate.FromInstructionToImmediate
import cores.lib.riscv.{InstructionType, Opcode}
import cores.modules.ALU.AluFunction
import cores.modules.{IntegerRegisterFile, PipelineStage}
import cores.nix.ControlTypes.{LeftOperand, RightOperand}

class Decode extends PipelineStage(new FetchToDecode, new DecodeToExecute) {

  val io = IO(new Bundle {
    val registerSources = Input(new IntegerRegisterFile.SourceResponse)
    val hazardDetection = new Hazard.DecodeChannel
    val exception = Output(new ExceptionBundle)
  })

  val immediate = lookUp(upstream.reg.instructionType) in (
    InstructionType.I -> upstream.reg.instruction.extractImmediate.iType,
    InstructionType.U -> upstream.reg.instruction.extractImmediate.uType,
    InstructionType.S -> upstream.reg.instruction.extractImmediate.sType,
    InstructionType.B -> upstream.reg.instruction.extractImmediate.bType,
    InstructionType.J -> upstream.reg.instruction.extractImmediate.jType
  )

  val source = VecInit(upstream.reg.instruction(19,15), upstream.reg.instruction(24,20))
  val destination = upstream.reg.instruction(11,7)
  val funct3 = upstream.reg.instruction(14,12)
  val (opcode,_) = Opcode.safe(upstream.reg.instruction(6,0))


  val operand = io.registerSources.data

  val isCsrAccess = upstream.reg.withSideEffects.isSystem && funct3 =/= 0.U

  import upstream.reg._
  import upstream.reg.withSideEffects._
  val leftOperand = MuxCase(LeftOperand.Register, Seq(
    (isAuipc || isJalr || isJal) -> LeftOperand.PC,
    isLui -> LeftOperand.Zero
  ))
  val rightOperand = MuxCase(RightOperand.Immediate, Seq(
    (isRegister || isBranch) -> RightOperand.Register,
    (isJalr || isJal) -> RightOperand.Four
  ))

  io.exception.set(
    _.cause := Exception.Cause.MachineSoftwareInterrupt,
    _.value := 0.U,
    _.exception := 0.B,
    _.pc := upstream.reg.pc
  )

  io.hazardDetection.source := source

  downstream.reg.set(
    _.pc := upstream.reg.pc,
    _.registerOperand := io.registerSources.data,
    _.csrIndex := immediate(11,0),
    _.immediate := immediate,
    _.source := source,
    _.destination := destination,
    _.funct3 := funct3,
    _.leftOperand := leftOperand,
    _.rightOperand := rightOperand,
    _.aluFunction := AluFunction.fromInstruction(upstream.reg.instruction),
    _.memoryOperation := MemoryOperation.fromOpcode(opcode),
    _.withSideEffects.set(
      _.isLoad := upstream.reg.withSideEffects.isLoad,
      _.isEcall := upstream.reg.instruction === 0x73.U,
      _.isBranch := upstream.reg.withSideEffects.isBranch,
      _.isJal := upstream.reg.withSideEffects.isJal,
      _.isJalr := upstream.reg.withSideEffects.isJalr,
      _.hasMemoryAccess := upstream.reg.withSideEffects.isLoad || upstream.reg.withSideEffects.isStore,
      _.isCsrWrite := isCsrAccess,
      _.isCsrRead := isCsrAccess && upstream.reg.destinationIsNonZero,
      _.hasRegisterWriteBack := upstream.reg.withSideEffects.hasRegisterWriteBack
    )
  )

  upstream.flowControl.set(
    _.stall := downstream.flowControl.stall || io.hazardDetection.hazard,
    _.flush := downstream.flowControl.flush
  )

  when(downstream.flowControl.flush || io.hazardDetection.hazard) {
    downstream.reg.withSideEffects.set(
      _.isEcall := 0.B,
      _.isLoad := 0.B,
      _.isBranch := 0.B,
      _.isJal := 0.B,
      _.isJalr := 0.B,
      _.hasMemoryAccess := 0.B,
      _.isCsrRead := 0.B,
      _.isCsrWrite := 0.B,
      _.hasRegisterWriteBack := 0.B
    )
  }


}
