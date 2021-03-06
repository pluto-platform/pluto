package cores.nix.stages

import chisel3._
import chisel3.util.{MuxCase, RegEnable}
import lib.util.BundleItemAssignment
import Interfaces._
import cores.lib.riscv.{InstructionType, Opcode}
import cores.modules.{IntegerRegisterFile, PipelineStage}
import cores.nix.ControlTypes.{LeftOperand, RightOperand}
import cores.nix.Pipeline.InstructionChannel


class Fetch extends PipelineStage(new ToFetch, new FetchToDecode) {

  val io = IO(new Bundle {
    val instructionResponse = Flipped(new InstructionChannel.Response)
    val registerSources = Output(new IntegerRegisterFile.SourceRequest)
  })

  val instruction = io.instructionResponse.bits.instruction

  val (opcode, validOpcode) = Opcode.safeFromInstruction(instruction)
  val source = VecInit(instruction(19,15), instruction(24,20))
  val destination = instruction(11,7)
  val funct3 = instruction(14,12)

  val isJal = opcode === Opcode.jal
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



  io.registerSources.index := Mux(downstream.flowControl.stall, RegEnable(source, VecInit(0.U(5.W),0.U(5.W)), !downstream.flowControl.stall), source)

  upstream.flowControl.set(
    _.stall := !io.instructionResponse.valid || downstream.flowControl.stall,
    _.flush := downstream.flowControl.flush
  )

  downstream.reg.set(
    _.pc := upstream.reg.pc,
    _.instruction := instruction,
    _.validOpcode := validOpcode,
    _.isLui := isLui,
    _.isAuipc := isAuipc,
    _.isImmediate := isImmediate,
    _.isRegister := isRegister,
    _.destinationIsNonZero := destinationIsNoneZero,
    _.instructionType := InstructionType.fromOpcode(opcode),
    _.withSideEffects.set(
      _.isJal := isJal,
      _.isJalr := isJalr,
      _.isBranch := isBranch,
      _.isLoad := isLoad,
      _.isStore := isStore,
      _.isSystem := isSystem,
      _.hasRegisterWriteBack := (!isStore && !isBranch) && destinationIsNoneZero,
      _.isBubble := 0.B
    )
  )
  // insert NOP when flushing or when starved by the instruction cache
  when(downstream.flowControl.flush || !io.instructionResponse.valid) {
    downstream.reg.set(
      _.instruction := 0x13.U,
      _.validOpcode := 1.B,
      _.withSideEffects.set(
        _.isJal := 0.B,
        _.isJalr := 0.B,
        _.isBranch := 0.B,
        _.isLoad := 0.B,
        _.isStore := 0.B,
        _.isSystem := 0.B,
        _.hasRegisterWriteBack := 0.B,
        _.isBubble := 1.B
      )
    )
  }

}









