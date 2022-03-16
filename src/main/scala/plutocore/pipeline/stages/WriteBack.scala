package plutocore.pipeline.stages

import chisel3._
import chisel3.util.Valid
import plutocore.pipeline.Pipeline.DataChannel
import plutocore.pipeline.PipelineInterfaces.MemoryToWriteBack
import plutocore.pipeline.{ControlAndStatusRegisterFile, Forwarding, IntegerRegisterFile, PipelineStage}
import lib.util.BundleItemAssignment

class WriteBack extends PipelineStage(new MemoryToWriteBack, new Bundle {}) {


  val io = IO(new Bundle {
    val forwarding = new Forwarding.WriteBackChannel
    val registerFile = Valid(new IntegerRegisterFile.WriteRequest)
    val csrFile = Valid(new ControlAndStatusRegisterFile.WriteRequest)
    val dataResponse = Flipped(new DataChannel.Response)
  })

  val writeBackValue = Mux(upstream.data.control.isLoad, io.dataResponse.bits.readData, upstream.data.registerWriteBack.value)

  upstream.flowControl.set(
    _.flush := 0.B,
    _.stall := upstream.data.control.isLoad && !io.dataResponse.valid
  )

  io.registerFile.set(
    _.valid := upstream.data.control.withSideEffects.writeRegisterFile,
    _.bits.index := upstream.data.registerWriteBack.index,
    _.bits.data := writeBackValue
  )

  io.csrFile.set(
    _.valid := upstream.data.control.withSideEffects.writeCsrFile,
    _.bits.index := upstream.data.csrWriteBack.index,
    _.bits.value := upstream.data.csrWriteBack.value
  )


  io.forwarding.set(
    _.destination := upstream.data.registerWriteBack.index,
    _.canForward := upstream.data.control.withSideEffects.writeRegisterFile,
    _.value := writeBackValue
  )


}
object Emitter extends App {
  emitVerilog(new WriteBack)
}