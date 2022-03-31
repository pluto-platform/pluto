package cores.nix.stages

import chisel3._
import chisel3.util.Valid
import lib.util.BundleItemAssignment
import cores.nix
import cores.nix.Forwarding
import Interfaces.MemoryToWriteBack
import cores.modules.{ControlAndStatusRegisterFile, IntegerRegisterFile, PipelineStage}
import cores.nix.Pipeline.DataChannel
import cores.lib.Exception

class WriteBack extends PipelineStage(new MemoryToWriteBack, new Bundle {}) {


  val io = IO(new Bundle {
    val forwarding = new Forwarding.WriteBackChannel
    val registerFile = Valid(new IntegerRegisterFile.WriteRequest)
    val csrFile = Valid(new ControlAndStatusRegisterFile.WriteRequest)
    val dataResponse = Flipped(new DataChannel.Response)
    val exception = Output(new Exception.ExceptionBundle)
    val instructionRetired = Output(Bool())
  })

  val writeBackValue = Mux(upstream.data.control.isLoad, io.dataResponse.bits.readData, upstream.data.registerWriteBack.value)

  upstream.flowControl.set(
    _.flush := 0.B,
    _.stall := upstream.data.control.isLoad && !io.dataResponse.valid
  )

  io.exception.set(
    _.cause := Exception.Cause.None,
    _.exception := 0.B,
    _.value := 0.U,
    _.pc := upstream.data.pc
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
    _.value := upstream.data.registerWriteBack.value // TODO: add additional stall
  )

  io.instructionRetired := !(upstream.data.control.isLoad && !io.dataResponse.valid)


}
object Emitter extends App {
  emitVerilog(new WriteBack)
}