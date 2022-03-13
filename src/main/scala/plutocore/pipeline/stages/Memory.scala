package plutocore.pipeline.stages

import chisel3._
import plutocore.pipeline.ControlTypes.{BitMaskerFunction, MemoryAccessWidth, MemoryOperation}
import plutocore.pipeline.Pipeline.DataChannel
import plutocore.pipeline.PipelineInterfaces.{ExecuteToMemory, MemoryToWriteBack}
import plutocore.pipeline.{BitMasker, ControlAndStatusRegisterFile, Forwarding, Hazard, PipelineStage}
import lib.util.BundleItemAssignment

class Memory extends PipelineStage(new ExecuteToMemory, new MemoryToWriteBack) {

  val io = IO(new Bundle {

    val forwarding = new Forwarding.MemoryChannel
    val dataRequest = new DataChannel.Request
    val csrResponse = Input(new ControlAndStatusRegisterFile.ReadResponse)
    val hazardDetection = new Hazard.MemoryChannel

  })

  val (bitMaskerFunction,_) = BitMaskerFunction.safe(upstream.data.funct3(1,0))
  val (memoryAccessWidth,_) = MemoryAccessWidth.safe(upstream.data.funct3(1,0))

  val bitMasker = Module(new BitMasker)
  bitMasker.io.set(
    _.operand(0) := io.csrResponse.value,
    _.operand(1) := upstream.data.writeValue,
    _.function := bitMaskerFunction
  )

  val memNotReady = !io.dataRequest.ready && upstream.data.control.withSideEffects.hasMemoryAccess

  upstream.flowControl.set(
    _.flush := downstream.flowControl.flush,
    _.stall := downstream.flowControl.stall || memNotReady
  )

  io.hazardDetection.set(
    _.destination := upstream.data.destination,
    _.isLoad := upstream.data.control.isLoad,
    _.canForward := upstream.data.control.withSideEffects.hasRegisterWriteBack
  )

  io.forwarding.set(
    _.destination := upstream.data.destination,
    _.canForward := upstream.data.control.withSideEffects.hasRegisterWriteBack,
    _.value := upstream.data.aluResult
  )

  io.dataRequest.set(
    _.valid := upstream.data.control.withSideEffects.hasMemoryAccess,
    _.bits.set(
      _.address := upstream.data.aluResult,
      _.writeData := upstream.data.writeValue,
      _.op := upstream.data.control.memoryOperation,
      _.accessWidth := memoryAccessWidth
    )
  )

  downstream.data.set(
    _.pc := upstream.data.pc,
    _.csrWriteBack.value := bitMasker.io.result,
    _.csrWriteBack.index := upstream.data.csrIndex,
    _.registerWriteBack.value := Mux(upstream.data.control.withSideEffects.isCsrWrite, io.csrResponse.value, upstream.data.aluResult),
    _.registerWriteBack.index := upstream.data.destination,
    _.control.set(
      _.isLoad := upstream.data.control.isLoad,
      _.withSideEffects.set(
        _.writeCsrFile := upstream.data.control.withSideEffects.isCsrWrite,
        _.writeRegisterFile := upstream.data.control.withSideEffects.hasRegisterWriteBack
      )
    )
  )

  when(memNotReady) {
    downstream.data.control.withSideEffects.set(
      _.writeCsrFile := 0.B,
      _.writeRegisterFile := 0.B
    )
  }

}

