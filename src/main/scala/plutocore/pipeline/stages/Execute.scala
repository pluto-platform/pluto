package plutocore.pipeline.stages

import chisel3._
import chisel3.util.Valid
import plutocore.pipeline.ControlTypes.AluFunction
import plutocore.pipeline.PipelineInterfaces.{DecodeToExecute, ExecuteToMemory}
import plutocore.pipeline.{ALU, ControlAndStatusRegisterFile, Forwarding, Hazard, PipelineStage}
import lib.util.BundleItemAssignment

// TODO: add csr access stall here or in decode

class Execute extends PipelineStage(new DecodeToExecute, new ExecuteToMemory) {


  val io = IO(new Bundle {
    val forwarding = new Forwarding.ExecuteChannel
    val csrRequest = Valid(new ControlAndStatusRegisterFile.ReadRequest)
    val hazardDetection = new Hazard.ExecuteChannel

  })

  (io.forwarding.channel, upstream.data.source)
    .zipped
    .foreach { case (channel, source) => channel.source := source}
  val operand = (upstream.data.operand, io.forwarding.channel, upstream.data.control.acceptsForwarding)
    .zipped
    .map { case (reg, channel, accepts) => Mux(channel.shouldForward && accepts, channel.value, reg)}
  val writeBackValue = Mux(upstream.data.control.withSideEffects.isCsrWrite && io.forwarding.channel(0).shouldForward, io.forwarding.channel(0).value, Mux( // TODO: when imm csr then source(1) needs to go here
    !upstream.data.control.withSideEffects.isCsrWrite && io.forwarding.channel(1).shouldForward, io.forwarding.channel(1).value, upstream.data.writeValue)
  )

  val alu = Module(new ALU)
  alu.io.set(
    _.operand := operand,
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
    _.control.set(
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


}

object ExecuteEmitter extends App {
  emitVerilog(new Execute)
}