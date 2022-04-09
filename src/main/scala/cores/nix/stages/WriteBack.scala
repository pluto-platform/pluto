package cores.nix.stages

import chisel3._
import chisel3.util.{Fill, MuxCase, MuxLookup, Valid}
import lib.util.BundleItemAssignment
import cores.nix
import cores.nix.Forwarding
import Interfaces.MemoryToWriteBack
import cores.lib.ControlTypes.MemoryAccessWidth
import cores.modules.{ControlAndStatusRegisterFile, IntegerRegisterFile, PipelineStage}
import cores.nix.Pipeline.DataChannel
import cores.lib.Exception
import lib.LookUp.lookUp

class WriteBack extends PipelineStage(new MemoryToWriteBack, new Bundle {}) {


  val io = IO(new Bundle {
    val forwarding = new Forwarding.WriteBackChannel
    val registerFile = Valid(new IntegerRegisterFile.WriteRequest)
    val csrFile = Valid(new ControlAndStatusRegisterFile.WriteRequest)
    val dataResponse = Flipped(new DataChannel.Response)
    val exception = Output(new Exception.ExceptionBundle)
    val instructionRetired = Output(Bool())
    val ecallRetired = Output(Bool())
  })


  val loadValue = lookUp(upstream.data.accessWidth) in (
    MemoryAccessWidth.Word -> io.dataResponse.bits.readData,
    MemoryAccessWidth.HalfWord -> Mux(upstream.data.signed, Fill(16, io.dataResponse.bits.readData(15)), Fill(16, 0.B)) ## io.dataResponse.bits.readData(15,0),
    MemoryAccessWidth.Byte -> Mux(upstream.data.signed, Fill(24, io.dataResponse.bits.readData(7)), Fill(24, 0.B)) ## io.dataResponse.bits.readData(7,0)
  )

  val writeBackValue = Mux(upstream.data.control.isLoad, loadValue, upstream.data.registerWriteBack.value)

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
    _.value := writeBackValue
  )

  io.instructionRetired := !(upstream.data.control.isLoad && !io.dataResponse.valid)
  io.ecallRetired := upstream.data.control.isEcall

}