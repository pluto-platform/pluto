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

  control.upstream.set(
    _.flush := control.downstream.flush,
    _.stall := control.downstream.stall || !io.dataRequest.ready
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

  downstream.set(
    _.pc := upstream.pc,
    _.destination := upstream.destination,
    _.csrWriteBack.value := bitMasker.io.result,
    _.csrWriteBack.index := upstream.csrIndex,
    _.registerWriteBack := Mux(upstream.control.isCsrAccess, io.csrResponse.value, upstream.aluResult),
    _.control.set(
      _.isLoad := upstream.control.isLoad,
      _.write.csr := upstream.control.isCsrAccess,
      _.write.registerFile := upstream.control.hasRegisterWriteBack
    )
  )

  when(!io.dataRequest.ready) {
    downstream.control.set(
      _.write.csr := 0.B,
      _.write.registerFile := 0.B
    )
  }

}

object Emitter extends App {
  emitVerilog(new Memory)
}