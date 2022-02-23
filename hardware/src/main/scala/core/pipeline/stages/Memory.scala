package core.pipeline.stages

import chisel3._
import core.ControlTypes.{BitMaskerFunction, MemoryAccessWidth, MemoryOperation}
import core.Pipeline.DataChannel
import core.PipelineInterfaces.{ExecuteToMemory, MemoryToWriteBack}
import core.pipeline.{BitMasker, ControlAndStatusRegisterFile}
import core.{Forwarding, PipelineStage}
import lib.util.BundleItemAssignment

class Memory extends PipelineStage(new ExecuteToMemory, new MemoryToWriteBack) {

  val io = IO(new Bundle {

    val forwarding = new Forwarding.ProviderChannel
    val dataRequest = new DataChannel.Request
    val csrResponse = Input(new ControlAndStatusRegisterFile.ReadResponse)

  })

  val (bitMaskerFunction,_) = BitMaskerFunction.safe(upstream.data.funct3(1,0))
  val (memoryAccessWidth,_) = MemoryAccessWidth.safe(upstream.data.funct3(1,0))

  val bitMasker = Module(new BitMasker)
  bitMasker.io.set(
    _.operand(0) := io.csrResponse.value,
    _.operand(1) := upstream.data.writeValue,
    _.function := bitMaskerFunction
  )

  upstream.flowControl.set(
    _.flush := downstream.flowControl.flush,
    _.stall := downstream.flowControl.stall || !io.dataRequest.ready
  )

  io.forwarding.set(
    _.destination := upstream.data.destination,
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

  when(!io.dataRequest.ready) {
    downstream.data.control.withSideEffects.set(
      _.writeCsrFile := 0.B,
      _.writeRegisterFile := 0.B
    )
  }

}

