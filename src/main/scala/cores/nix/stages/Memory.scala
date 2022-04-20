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

  val bitMaskerFunction = BitMaskerFunction.fromFunct3(upstream.reg.funct3(1,0))
  val memoryAccessWidth = MemoryAccessWidth.fromFunct3(upstream.reg.funct3(1,0))

  val bitMasker = Module(new BitMasker)
  bitMasker.io.set(
    _.operand(0) := io.csrResponse.value,
    _.operand(1) := upstream.reg.writeValue,
    _.function := bitMaskerFunction
  )

  val memNotReady = !io.dataRequest.ready && upstream.reg.withSideEffects.hasMemoryAccess

  io.branching.set(
    _.jump := upstream.reg.withSideEffects.jump,
    _.target := upstream.reg.target
  )

  upstream.flowControl.set(
    _.flush := downstream.flowControl.flush || upstream.reg.withSideEffects.jump,
    _.stall := downstream.flowControl.stall || memNotReady
  )

  io.forwarding.set(
    _.destination := upstream.reg.destination,
    _.canForward := upstream.reg.withSideEffects.hasRegisterWriteBack && !upstream.reg.withSideEffects.isLoad,
    _.value := upstream.reg.aluResult
  )

  io.dataRequest.set(
    _.valid := upstream.reg.withSideEffects.hasMemoryAccess,
    _.bits.set(
      _.address := upstream.reg.aluResult,
      _.writeData := upstream.reg.writeValue,
      _.op := upstream.reg.memoryOperation,
      _.accessWidth := memoryAccessWidth
    )
  )

  downstream.reg.set(
    _.pc := upstream.reg.pc,
    _.csrWriteBack.value := bitMasker.io.result,
    _.csrWriteBack.index := upstream.reg.csrIndex,
    _.registerWriteBack.value := Mux(upstream.reg.withSideEffects.isCsrWrite, io.csrResponse.value, upstream.reg.aluResult),
    _.registerWriteBack.index := upstream.reg.destination,
    _.accessWidth := memoryAccessWidth,
    _.signed := !upstream.reg.funct3(2),
    _.withSideEffects.set(
      _.isLoad := upstream.reg.withSideEffects.isLoad,
      _.isEcall := upstream.reg.withSideEffects.isEcall,
      _.writeCsrFile := upstream.reg.withSideEffects.isCsrWrite,
      _.writeRegisterFile := upstream.reg.withSideEffects.hasRegisterWriteBack
    )
  )

  when(memNotReady || downstream.flowControl.flush) {
    downstream.reg.withSideEffects.set(
      _.isLoad := 0.B,
      _.isEcall := 0.B,
      _.writeCsrFile := 0.B,
      _.writeRegisterFile := 0.B
    )
  }

}

