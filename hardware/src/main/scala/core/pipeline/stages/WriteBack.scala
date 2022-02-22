package core.pipeline.stages

import chisel3._
import chisel3.util.Valid
import core.Pipeline.DataChannel
import core.PipelineInterfaces.MemoryToWriteBack
import core.pipeline.{ControlAndStatusRegisterFile, IntegerRegisterFile}
import core.{Forwarding, PipelineStage}
import lib.util.BundleItemAssignment

class WriteBack extends PipelineStage(new MemoryToWriteBack, new Bundle {}) {


  val io = IO(new Bundle {
    val forwarding = new Forwarding.ProviderChannel
    val registerFile = Valid(new IntegerRegisterFile.WriteRequest)
    val csrFile = Valid(new ControlAndStatusRegisterFile.WriteRequest)
    val dataResponse = Flipped(new DataChannel.Response)
  })


  val writeBackValue = Mux(upstream.control.isLoad, io.dataResponse.bits.readData, upstream.registerWriteBack.value)

  control.upstream.set(
    _.flush := 0.B,
    _.stall := upstream.control.isLoad && !io.dataResponse.valid
  )

  io.registerFile.set(
    _.valid := upstream.control.writeRegisterFile,
    _.bits.index := upstream.registerWriteBack.index,
    _.bits.data := writeBackValue
  )

  io.csrFile.set(
    _.valid := upstream.control.writeCsrFile,
    _.bits.index := upstream.csrWriteBack.index,
    _.bits.value := upstream.csrWriteBack.value
  )

  io.forwarding.set(
    _.destination := upstream.registerWriteBack.index,
    _.value := writeBackValue
  )


}
object Emitter extends App {
  emitVerilog(new WriteBack)
}