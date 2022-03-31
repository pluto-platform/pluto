package cores.nix.stages

import chisel3._
import lib.LookUp._
import lib.util.BundleItemAssignment
import cores.lib.ControlTypes._
import cores.nix.Hazard
import cores.lib.Exception
import Interfaces.{DecodeToExecute, FetchToDecode}
import chisel3.util.{MuxLookup, UIntToOH}
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
  )
  val source = VecInit(upstream.data.instruction(19,15), upstream.data.instruction(24,20))
  val destination = upstream.data.instruction(11,7)
  val funct3 = upstream.data.instruction(14,12)
  val (opcode,_) = Opcode.safe(upstream.data.instruction(6,0))


  val operand = io.registerSources.data

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


  val offset = MuxLookup(upstream.data.instruction(3,2), upstream.data.instruction.extractImmediate.bType, Seq(
    1.U -> upstream.data.instruction.extractImmediate.iType,
    3.U -> upstream.data.instruction.extractImmediate.jType
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
    _.operand := aluOperand,
    _.csrIndex := immediate(11,0),
    _.offset := offset,
    _.writeValue := operand(upstream.data.control.writeSourceRegister),
    _.source := source,
    _.destination := destination,
    _.funct3 := funct3,
    _.control.set(
      _.isLoad := upstream.data.control.isLoad,
      _.isBranch := upstream.data.control.isBranch,
      _.isJal := upstream.data.control.isJal,
      _.isJalr := upstream.data.control.isJalr,
      _.acceptsForwarding(0) := upstream.data.control.leftOperand === LeftOperand.Register,
      _.acceptsForwarding(1) := upstream.data.control.rightOperand === RightOperand.Register,
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