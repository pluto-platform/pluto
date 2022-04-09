package cores.nix.stages

import chisel3._
import lib.util.BundleItemAssignment
import cores.modules.{BitMasker, ControlAndStatusRegisterFile, PipelineStage}
import cores.lib.ControlTypes.MemoryAccessWidth
import cores.nix.{Branching, Forwarding, Hazard}
import Interfaces.{ExecuteToMemory, MemoryToWriteBack}
import cores.modules.BitMasker.BitMaskerFunction
import cores.nix.Pipeline.DataChannel

class Memory extends PipelineStage(new ExecuteToMemory, new MemoryToWriteBack) {

  val io = IO(new Bundle {

    val forwarding = new Forwarding.MemoryChannel
    val dataRequest = new DataChannel.Request
    val csrResponse = Input(new ControlAndStatusRegisterFile.ReadResponse)
    val branching = Flipped(new Branching.ProgramCounterChannel)

  })

  val bitMaskerFunction = BitMaskerFunction.fromFunct3(upstream.data.funct3(1,0))
  val memoryAccessWidth = MemoryAccessWidth.fromFunct3(upstream.data.funct3(1,0))

  val bitMasker = Module(new BitMasker)
  bitMasker.io.set(
    _.operand(0) := io.csrResponse.value,
    _.operand(1) := upstream.data.writeValue,
    _.function := bitMaskerFunction
  )

  val memNotReady = !io.dataRequest.ready && upstream.data.control.withSideEffects.hasMemoryAccess

  io.branching.set(
    _.jump := upstream.data.jump,
    _.target := upstream.data.target
  )

  upstream.flowControl.set(
    _.flush := downstream.flowControl.flush || upstream.data.jump,
    _.stall := downstream.flowControl.stall || memNotReady
  )

  io.forwarding.set(
    _.destination := upstream.data.destination,
    _.canForward := upstream.data.control.withSideEffects.hasRegisterWriteBack && !upstream.data.control.isLoad,
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
    _.accessWidth := memoryAccessWidth,
    _.signed := !upstream.data.funct3(2),
    _.control.set(
      _.isEcall := upstream.data.control.isEcall,
      _.isLoad := upstream.data.control.isLoad,
      _.withSideEffects.set(
        _.writeCsrFile := upstream.data.control.withSideEffects.isCsrWrite,
        _.writeRegisterFile := upstream.data.control.withSideEffects.hasRegisterWriteBack
      )
    )
  )

  when(memNotReady || downstream.flowControl.flush) {
    downstream.data.control.isEcall := 0.B
    downstream.data.control.withSideEffects.set(
      _.writeCsrFile := 0.B,
      _.writeRegisterFile := 0.B
    )
  }

}

