package core.pipeline.stages

import chisel3._
import chisel3.util.Valid
import core.ControlTypes.AluFunction
import core.PipelineInterfaces.{DecodeToExecute, ExecuteToMemory}
import core.pipeline.{ALU, ControlAndStatusRegisterFile}
import core.{Branching, Forwarding, Hazard, PipelineStage}
import lib.util.BundleItemAssignment

class Execute extends PipelineStage(new DecodeToExecute, new ExecuteToMemory) {


  val io = IO(new Bundle {
    val branching = new Branching.ExecuteChannel
    val forwarding = new Forwarding.ExecuteChannel
    val csrRequest = Valid(new ControlAndStatusRegisterFile.ReadRequest)
  })

  val op = VecInit(
    Mux(
      io.forwarding.shouldForward(0) && upstream.data.control.allowForwarding(0), // forward if register operand is used
      io.forwarding.forwardedValue,
      upstream.data.operand(0)
    ),
    Mux(
      io.forwarding.shouldForward(1) && upstream.data.control.allowForwarding(1), // forward if register operand is used
      io.forwarding.forwardedValue,
      upstream.data.operand(1)
    )
  )

  val alu = Module(new ALU)
  alu.io.set(
    _.operand := op,
    _.operation := upstream.data.control.aluFunction
  )


  io.branching.set(
    _.pc := upstream.data.pc,
    _.recoveryTarget := upstream.data.recoveryTarget,
    _.guess := upstream.data.control.guess,
    _.decision := branch
  )

  io.csrRequest.set(
    _.valid := upstream.data.control.withSideEffects.isCsrRead,
    _.bits.index := upstream.data.csrIndex
  )

  /*
  io.loadUseHazard.set(
    _.isLoad := upstream.data.control.isLoad,
    _.destination := upstream.data.destination
  )*/

  downstream.data.set(
    _.pc := upstream.data.pc,
    _.destination := upstream.data.destination,
    _.aluResult := alu.io.result,
    _.writeValue := upstream.data.writeValue,
    _.csrIndex := upstream.data.csrIndex,
    _.funct3 := upstream.data.funct3,
    _.control.set(
      _.isLoad := upstream.data.control.isLoad,
      _.destinationIsNonZero := upstream.data.control.destinationIsNonZero,
      _.memoryOperation := upstream.data.control.memoryOperation,
      _.withSideEffects.set(
        _.hasMemoryAccess := upstream.data.control.withSideEffects.hasMemoryAccess,
        _.isCsrWrite := upstream.data.control.withSideEffects.isCsrWrite,
        _.hasRegisterWriteBack := upstream.data.control.withSideEffects.hasRegisterWriteBack
      )
    )
  )

  upstream.flowControl.set(
    _.stall := downstream.flowControl.stall || upstream.data.control.withSideEffects.isCsrWrite,
    _.flush := downstream.flowControl.flush || branch =/= upstream.data.control.guess
  )


}

object ExecuteEmitter extends App {
  emitVerilog(new Execute)
}
