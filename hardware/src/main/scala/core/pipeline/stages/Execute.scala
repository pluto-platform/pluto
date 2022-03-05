package core.pipeline.stages

import chisel3._
import chisel3.util.Valid
import core.ControlTypes.AluFunction
import core.PipelineInterfaces.{DecodeToExecute, ExecuteToMemory}
import core.pipeline.{ALU, ControlAndStatusRegisterFile}
import core.{Branching, Forwarding, Hazard, PipelineStage}
import lib.util.BundleItemAssignment

// TODO: add csr access stall here or in decode

class Execute extends PipelineStage(new DecodeToExecute, new ExecuteToMemory) {


  val io = IO(new Bundle {
    val forwarding = new Forwarding.ExecuteChannel
    val csrRequest = Valid(new ControlAndStatusRegisterFile.ReadRequest)
  })

  val operand = upstream.data.operand.zip(io.forwarding.channel).map { case (reg, channel) => Mux(channel.shouldForward, channel.value, reg)}

  val alu = Module(new ALU)
  alu.io.set(
    _.operand := operand,
    _.operation := upstream.data.control.aluFunction
  )

  io.forwarding.nextMemoryInfo.set(
    _.destination := upstream.data.destination,
    _.canForward := upstream.data.control.withSideEffects.hasRegisterWriteBack
  )

  io.csrRequest.set(
    _.valid := upstream.data.control.withSideEffects.isCsrRead,
    _.bits.index := upstream.data.csrIndex
  )

  downstream.data.set(
    _.pc := upstream.data.pc,
    _.destination := upstream.data.destination,
    _.aluResult := alu.io.result,
    _.writeValue := upstream.data.writeValue,
    _.csrIndex := upstream.data.csrIndex,
    _.funct3 := upstream.data.funct3,
    _.control.set(
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
