package core.pipeline.stages

import chisel3._
import core.ControlTypes.MemoryOperation
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

  val bitMasker = Module(new BitMasker)
  bitMasker.io.set(
    _.operand(0) := io.csrResponse.value,
    _.operand(1) := upstream.writeData,
    _.function := upstream.control.bitMaskerFunction
  )

  io.forwarding.set(
    _.destination := upstream.destination,
    _.value := upstream.aluResult
  )

  io.dataRequest.set(
    _.valid := upstream.control.hasMemoryAccess,
    _.bits.set(
      _.address := upstream.aluResult,
      _.writeData := upstream.writeData,
      _.op := upstream.control.memory.memoryOperation,
      _.accessWidth := upstream.control.memory.memoryAccessWidth
    )
  )

  downstream <> upstream
  downstream.set(
    _.csrWriteBack := bitMasker.io.result
  )

}