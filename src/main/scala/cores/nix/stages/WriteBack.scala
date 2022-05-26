package cores.nix.stages

import chisel3._
import chisel3.util.{Fill, MuxCase, MuxLookup, Valid}
import lib.util.BundleItemAssignment
import cores.nix
import cores.nix.{ExceptionUnit, Forwarding}
import Interfaces.MemoryToWriteBack
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryAccessWidth, MemoryOperation}
import cores.modules.{ControlAndStatusRegisterFile, IntegerRegisterFile, PipelineStage}
import cores.nix.Pipeline.DataChannel
import cores.lib.Exception
import cores.lib.Exception.Cause
import lib.LookUp.lookUp

class WriteBack extends PipelineStage(new MemoryToWriteBack, new Bundle {}) {


  val io = IO(new Bundle {
    val forwarding = new Forwarding.WriteBackChannel
    val registerFile = Valid(new IntegerRegisterFile.WriteRequest)
    val csrFile = Valid(new ControlAndStatusRegisterFile.WriteRequest)
    val dataResponse = Flipped(new DataChannel.Response)
    val exception = new ExceptionUnit.WriteBackChannel
    val instructionRetired = Output(Bool())
    val ecallRetired = Output(Bool())
  })


  val loadValue = lookUp(upstream.reg.accessWidth) in (
    MemoryAccessWidth.Word -> io.dataResponse.bits.readData,
    MemoryAccessWidth.HalfWord -> Mux(upstream.reg.signed, Fill(16, io.dataResponse.bits.readData(15)), Fill(16, 0.B)) ## io.dataResponse.bits.readData(15,0),
    MemoryAccessWidth.Byte -> Mux(upstream.reg.signed, Fill(24, io.dataResponse.bits.readData(7)), Fill(24, 0.B)) ## io.dataResponse.bits.readData(7,0)
  )

  val writeBackValue = Mux(upstream.reg.withSideEffects.isLoad, loadValue, upstream.reg.registerWriteBack.value)

  val memoryAccessError =  io.dataResponse.valid && io.dataResponse.bits.result === MemoryAccessResult.Failure

  upstream.flowControl.set(
    _.flush := io.exception.flush,
    _.stall := (upstream.reg.withSideEffects.hasMemoryAccess && !io.dataResponse.valid) || downstream.flowControl.stall
  )
  io.dataResponse.ready := !downstream.flowControl.stall

  val cause = MuxCase(upstream.reg.cause, Seq(
    upstream.reg.withSideEffects.exception -> upstream.reg.cause,
    memoryAccessError -> Mux(upstream.reg.memoryOperation === MemoryOperation.Read, Cause.LoadAccessFault, Cause.StoreAccessFault)
  ))

  io.exception.set(
    _.exception.set(
      _.exception := upstream.reg.withSideEffects.exception || memoryAccessError,
      _.cause := cause,
      _.pc := upstream.reg.pc,
      _.value := 0.U
    ),
    _.mret := upstream.reg.withSideEffects.isMret,
    _.isBubble := upstream.reg.withSideEffects.isBubble || upstream.reg.withSideEffects.jumped
  )

  io.registerFile.set(
    _.valid := upstream.reg.withSideEffects.writeRegisterFile && !downstream.flowControl.stall,
    _.bits.index := upstream.reg.registerWriteBack.index,
    _.bits.data := writeBackValue
  )

  io.csrFile.set(
    _.valid := upstream.reg.withSideEffects.writeCsrFile && !downstream.flowControl.stall,
    _.bits.index := upstream.reg.csrWriteBack.index,
    _.bits.value := upstream.reg.csrWriteBack.value
  )


  io.forwarding.set(
    _.destination := upstream.reg.registerWriteBack.index,
    _.canForward := upstream.reg.withSideEffects.writeRegisterFile && !(upstream.reg.withSideEffects.hasMemoryAccess && upstream.reg.memoryOperation === MemoryOperation.Write),
    _.value := writeBackValue
  )

  io.instructionRetired := !(upstream.reg.withSideEffects.isLoad && !io.dataResponse.valid)
  io.ecallRetired := upstream.reg.withSideEffects.isEcall

}