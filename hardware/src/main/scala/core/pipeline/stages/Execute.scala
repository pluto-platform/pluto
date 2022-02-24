package core.pipeline.stages

import chisel3._
import chisel3.util.Valid
import core.ControlTypes.AluFunction
import core.PipelineInterfaces.{DecodeToExecute, ExecuteToMemory}
import core.pipeline.{ALU, ControlAndStatusRegisterFile}
import core.{Forwarding, LoadUseHazard, PipelineStage}
import lib.util.BundleItemAssignment

class Execute extends PipelineStage(new DecodeToExecute, new ExecuteToMemory) {


  val io = IO(new Bundle {
    val forwarding = new Forwarding.ExecuteChannel
    val csrRequest = Valid(new ControlAndStatusRegisterFile.ReadRequest)
    val loadUseHazard = new LoadUseHazard.ExecuteChannel
  })

  val (aluFunction,_) = AluFunction.safe(upstream.data.funct7_5 ## upstream.data.funct3)

  val alu = Module(new ALU)
  alu.io.set(
    _.operand(0) := Mux(
      io.forwarding.shouldForward(0) && upstream.data.control.allowForwarding(0), // forward if register operand is used
      io.forwarding.value,
      upstream.data.operand(0)
    ),
    _.operand(1) := Mux(
      io.forwarding.shouldForward(1) && upstream.data.control.allowForwarding(1), // forward if register operand is used
      io.forwarding.value,
      upstream.data.operand(1)
    ),
    _.operation := aluFunction
  )

  io.forwarding.source := upstream.data.source

  io.csrRequest.set(
    _.valid := upstream.data.control.withSideEffects.isCsrRead,
    _.bits.index := upstream.data.csrIndex
  )

  io.loadUseHazard.set(
    _.isLoad := upstream.data.control.isLoad,
    _.destination := upstream.data.destination
  )

  downstream.data.set(
    _.pc := upstream.data.pc,
    _.destination := upstream.data.destination,
    _.aluResult := alu.io.result,
    _.writeValue := upstream.data.writeValue,
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


}

object ExecuteEmitter extends App {
  emitVerilog(new Execute)
}
