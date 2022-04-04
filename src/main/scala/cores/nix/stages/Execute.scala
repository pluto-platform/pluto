package cores.nix.stages

import chisel3._
import chisel3.util.{MuxCase, MuxLookup, Valid}
import lib.util.{BoolVec, BundleItemAssignment}
import cores.modules.{ALU, ControlAndStatusRegisterFile, PipelineStage}
import cores.nix.{Branching, Forwarding, Hazard}
import Interfaces.{DecodeToExecute, ExecuteToMemory}
import cores.nix.ControlTypes.{LeftOperand, RightOperand}
import lib.LookUp.lookUp

// TODO: add csr access stall here or in decode

class Execute extends PipelineStage(new DecodeToExecute, new ExecuteToMemory) {


  val io = IO(new Bundle {
    val forwarding = new Forwarding.ExecuteChannel
    val csrRequest = Valid(new ControlAndStatusRegisterFile.ReadRequest)
    val hazardDetection = new Hazard.ExecuteChannel

  })



  (io.forwarding.channel, upstream.data.source)
    .zipped
    .foreach { case (channel, source) => channel.source := source }

  val registerOperand = (upstream.data.registerOperand, io.forwarding.channel)
    .zipped
    .map { case (reg, channel) => Mux(channel.shouldForward, channel.value, reg) }

  val aluOperand = VecInit(
    lookUp(upstream.data.control.leftOperand) in (
      LeftOperand.Register -> registerOperand(0),
      LeftOperand.PC -> upstream.data.pc,
      LeftOperand.Zero -> 0.U
    ),
    lookUp(upstream.data.control.rightOperand) in (
      RightOperand.Register -> registerOperand(1),
      RightOperand.Immediate -> upstream.data.immediate.asUInt,
      RightOperand.Four -> 4.U
    )
  )



  val writeBackValue = Mux(upstream.data.control.withSideEffects.isCsrWrite,
    Mux(upstream.data.funct3(2), upstream.data.source(0), registerOperand(0)), // distinguish between imm csr and non imm csr
    registerOperand(1))

  val comparison = MuxLookup(upstream.data.funct3, 0.B, Seq(
    "b000".U -> (registerOperand(0) === registerOperand(1)),
    "b001".U -> (registerOperand(0) =/= registerOperand(1)),
    "b100".U -> (registerOperand(0).asSInt < registerOperand(1).asSInt),
    "b101".U -> (registerOperand(0).asSInt >= registerOperand(1).asSInt),
    "b110".U -> (registerOperand(0) < registerOperand(1)),
    "b111".U -> (registerOperand(0) >= registerOperand(1))
  ))


  val jump = upstream.data.control.isJalr || upstream.data.control.isJal || (upstream.data.control.isBranch && comparison)

  val target = (Mux(upstream.data.control.isJalr, registerOperand(0), upstream.data.pc).asSInt + upstream.data.immediate).asUInt

  val alu = Module(new ALU)
  alu.io.set(
    _.operand := aluOperand,
    _.operation := upstream.data.control.aluFunction
  )

  io.csrRequest.set(
    _.valid := upstream.data.control.withSideEffects.isCsrRead,
    _.bits.index := upstream.data.csrIndex
  )

  io.hazardDetection.set(
    _.isLoad := upstream.data.control.isLoad,
    _.destination := upstream.data.destination,
    _.canForward := upstream.data.control.withSideEffects.hasRegisterWriteBack
  )

  downstream.data.set(
    _.pc := upstream.data.pc,
    _.destination := upstream.data.destination,
    _.aluResult := alu.io.result,
    _.writeValue := writeBackValue,
    _.csrIndex := upstream.data.csrIndex,
    _.funct3 := upstream.data.funct3,
    _.jump := jump,
    _.target := target,
    _.control.set(
      _.isEcall := upstream.data.control.isEcall,
      _.isLoad := upstream.data.control.isLoad,
      _.memoryOperation := upstream.data.control.memoryOperation,
      _.withSideEffects.set(
        _.hasMemoryAccess := upstream.data.control.withSideEffects.hasMemoryAccess,
        _.isCsrWrite := upstream.data.control.withSideEffects.isCsrWrite,
        _.hasRegisterWriteBack := upstream.data.control.withSideEffects.hasRegisterWriteBack
      )
    )
  )

  upstream.flowControl.set(
    _.stall := downstream.flowControl.stall,
    _.flush := downstream.flowControl.flush
  )

  when(downstream.flowControl.flush) {
    downstream.data.control.isEcall := 0.B
    downstream.data.control.isLoad := 0.B
    downstream.data.jump := 0.B
    downstream.data.control.withSideEffects.set(
      _.hasRegisterWriteBack := 0.B,
      _.hasMemoryAccess := 0.B,
      _.isCsrWrite := 0.B
    )
  }


}

object ExecuteEmitter extends App {
  emitVerilog(new Execute)
}
