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



  (io.forwarding.channel, upstream.reg.source)
    .zipped
    .foreach { case (channel, source) => channel.source := source }

  val registerOperand = (upstream.reg.registerOperand, io.forwarding.channel)
    .zipped
    .map { case (reg, channel) => Mux(channel.shouldForward, channel.value, reg) }

  val aluOperand = VecInit(
    lookUp(upstream.reg.leftOperand) in (
      LeftOperand.Register -> registerOperand(0),
      LeftOperand.PC -> upstream.reg.pc,
      LeftOperand.Zero -> 0.U
    ),
    lookUp(upstream.reg.rightOperand) in (
      RightOperand.Register -> registerOperand(1),
      RightOperand.Immediate -> upstream.reg.immediate.asUInt,
      RightOperand.Four -> 4.U
    )
  )



  val writeBackValue = Mux(upstream.reg.withSideEffects.isCsrWrite,
    Mux(upstream.reg.funct3(2), upstream.reg.source(0), registerOperand(0)), // distinguish between imm csr and non imm csr
    registerOperand(1))

  val comparison = MuxLookup(upstream.reg.funct3, 0.B, Seq(
    "b000".U -> (registerOperand(0) === registerOperand(1)),
    "b001".U -> (registerOperand(0) =/= registerOperand(1)),
    "b100".U -> (registerOperand(0).asSInt < registerOperand(1).asSInt),
    "b101".U -> (registerOperand(0).asSInt >= registerOperand(1).asSInt),
    "b110".U -> (registerOperand(0) < registerOperand(1)),
    "b111".U -> (registerOperand(0) >= registerOperand(1))
  ))


  val jump = upstream.reg.withSideEffects.isJalr || upstream.reg.withSideEffects.isJal || (upstream.reg.withSideEffects.isBranch && comparison)

  val target = (Mux(upstream.reg.withSideEffects.isJalr, registerOperand(0), upstream.reg.pc).asSInt + upstream.reg.immediate).asUInt

  val alu = Module(new ALU)
  alu.io.set(
    _.operand := aluOperand,
    _.operation := upstream.reg.aluFunction
  )

  io.csrRequest.set(
    _.valid := upstream.reg.withSideEffects.isCsrRead,
    _.bits.index := upstream.reg.csrIndex
  )

  io.hazardDetection.set(
    _.isLoad := upstream.reg.withSideEffects.isLoad && !downstream.flowControl.flush,
    _.destination := upstream.reg.destination,
    _.canForward := upstream.reg.withSideEffects.hasRegisterWriteBack,
    _.bubble := upstream.reg.withSideEffects.exception || upstream.reg.withSideEffects.isMret,
    _.isCsr := upstream.reg.withSideEffects.isCsrWrite
  )

  downstream.reg.set(
    _.pc := upstream.reg.pc,
    _.destination := upstream.reg.destination,
    _.aluResult := alu.io.result,
    _.writeValue := writeBackValue,
    _.csrIndex := upstream.reg.csrIndex,
    _.funct3 := upstream.reg.funct3,
    _.target := target,
    _.memoryOperation := upstream.reg.memoryOperation,
    _.cause := upstream.reg.cause,
    _.withSideEffects.set(
      _.exception := upstream.reg.withSideEffects.exception,
      _.jump := jump,
      _.isMret := upstream.reg.withSideEffects.isMret,
      _.isLoad := upstream.reg.withSideEffects.isLoad,
      _.isEcall := upstream.reg.withSideEffects.isEcall,
      _.hasMemoryAccess := upstream.reg.withSideEffects.hasMemoryAccess,
      _.isCsrWrite := upstream.reg.withSideEffects.isCsrWrite,
      _.hasRegisterWriteBack := upstream.reg.withSideEffects.hasRegisterWriteBack,
      _.isBubble := upstream.reg.withSideEffects.isBubble
    )
  )

  upstream.flowControl.set(
    _.stall := downstream.flowControl.stall,
    _.flush := downstream.flowControl.flush
  )

  when(downstream.flowControl.flush) {
    downstream.reg.withSideEffects.set(
      _.exception := 0.B,
      _.isEcall := 0.B,
      _.isLoad := 0.B,
      _.isMret := 0.B,
      _.jump := 0.B,
      _.hasRegisterWriteBack := 0.B,
      _.hasMemoryAccess := 0.B,
      _.isCsrWrite := 0.B,
      _.isBubble := 1.B
    )
  }


}
