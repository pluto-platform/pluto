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

  val immediate = lookUp(upstream.data.control.instructionType) in (
    InstructionType.I -> upstream.data.instruction.extractImmediate.iType,
    InstructionType.U -> upstream.data.instruction.extractImmediate.uType,
    InstructionType.S -> upstream.data.instruction.extractImmediate.sType,
    InstructionType.B -> upstream.data.instruction.extractImmediate.bType,
    InstructionType.J -> upstream.data.instruction.extractImmediate.jType
  )

  val source = VecInit(upstream.data.instruction(19,15), upstream.data.instruction(24,20))
  val destination = upstream.data.instruction(11,7)
  val funct3 = upstream.data.instruction(14,12)
  val (opcode,_) = Opcode.safe(upstream.data.instruction(6,0))


  val operand = io.registerSources.data

  val isCsrAccess = upstream.data.control.isSystem && funct3 =/= 0.U

  import upstream.data.control._
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
    _.pc := upstream.data.pc
  )

  io.hazardDetection.source := source

  downstream.data.set(
    _.pc := upstream.data.pc,
    _.registerOperand := io.registerSources.data, //Mux(RegNext(io.hazardDetection.hazard, 0.B), RegNext(io.registerSources.data, VecInit(0.U,0.U)), io.registerSources.data),
    _.csrIndex := immediate(11,0),
    _.immediate := immediate,
    _.source := source,
    _.destination := destination,
    _.funct3 := funct3,
    _.control.set(
      _.isEcall := upstream.data.instruction === 0x73.U,
      _.isLoad := upstream.data.control.isLoad,
      _.isBranch := upstream.data.control.isBranch,
      _.isJal := upstream.data.control.isJal,
      _.isJalr := upstream.data.control.isJalr,
      _.leftOperand := leftOperand,
      _.rightOperand := rightOperand,
      _.aluFunction := AluFunction.fromInstruction(upstream.data.instruction),
      _.memoryOperation := MemoryOperation.fromOpcode(opcode),
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
    _.flush := downstream.flowControl.flush
  )

  when(downstream.flowControl.flush || io.hazardDetection.hazard) {
    downstream.data.control.isEcall := 0.B
    downstream.data.control.isLoad := 0.B
    downstream.data.control.isBranch := 0.B
    downstream.data.control.isJal := 0.B
    downstream.data.control.isJalr := 0.B
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