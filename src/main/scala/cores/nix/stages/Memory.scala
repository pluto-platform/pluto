package cores.nix.stages

import chisel3._
import lib.util.BundleItemAssignment
import cores.modules.{BitMasker, ControlAndStatusRegisterFile, PipelineStage}
import cores.lib.ControlTypes.{MemoryAccessWidth, MemoryOperation}
import cores.nix.{Branching, Forwarding, Hazard}
import Interfaces.{ExecuteToMemory, MemoryToWriteBack}
import chisel3.util.MuxCase
import cores.modules.BitMasker.BitMaskerFunction
import cores.nix.Pipeline.DataChannel
import cores.lib.Exception.Cause

class Memory extends PipelineStage(new ExecuteToMemory, new MemoryToWriteBack) {

  val io = IO(new Bundle {

    val forwarding = new Forwarding.MemoryChannel
    val dataRequest = new DataChannel.Request
    val csrResponse = Input(new ControlAndStatusRegisterFile.ReadResponse)
    val branching = Flipped(new Branching.ProgramCounterChannel)
    val hazard = new Hazard.MemoryChannel

  })

  val bitMaskerFunction = BitMaskerFunction.fromFunct3(upstream.reg.funct3(1,0))
  val memoryAccessWidth = MemoryAccessWidth.fromFunct3(upstream.reg.funct3(1,0))

  val bitMasker = Module(new BitMasker)
  bitMasker.io.set(
    _.operand(0) := io.csrResponse.value,
    _.operand(1) := upstream.reg.writeValue,
    _.function := bitMaskerFunction
  )

  val misalignedAddress =
    (memoryAccessWidth === MemoryAccessWidth.Word && upstream.reg.aluResult(1,0) =/= "b00".U) ||
      (memoryAccessWidth === MemoryAccessWidth.HalfWord && upstream.reg.aluResult(0) =/= "b0".U)

  val misalignmentException = misalignedAddress && upstream.reg.withSideEffects.hasMemoryAccess
  val exception = upstream.reg.withSideEffects.exception || misalignmentException
  val cause = MuxCase(upstream.reg.cause, Seq(
    upstream.reg.withSideEffects.exception -> upstream.reg.cause,
    misalignmentException -> Mux(upstream.reg.memoryOperation === MemoryOperation.Read, Cause.LoadAddressMisaligned, Cause.StoreAddressMisaligned)
  ))

  val memNotReady = !io.dataRequest.ready && upstream.reg.withSideEffects.hasMemoryAccess

  io.branching.set(
    _.jump := upstream.reg.withSideEffects.jump && !downstream.flowControl.stall && !downstream.flowControl.flush,
    _.target := upstream.reg.target
  )

  io.hazard.bubble := upstream.reg.withSideEffects.exception || upstream.reg.withSideEffects.isMret
  io.hazard.set(
    _.destination := upstream.reg.destination,
    _.canForward := upstream.reg.withSideEffects.hasRegisterWriteBack,
    _.isCsr := upstream.reg.withSideEffects.isCsrWrite
  )

  upstream.flowControl.set(
    _.flush := downstream.flowControl.flush || (!(downstream.flowControl.flush || downstream.flowControl.stall) && upstream.reg.withSideEffects.jump),
    _.stall := downstream.flowControl.stall || (!downstream.flowControl.flush && memNotReady)
  )

  io.forwarding.set(
    _.destination := upstream.reg.destination,
    _.canForward := upstream.reg.withSideEffects.hasRegisterWriteBack && !upstream.reg.withSideEffects.hasMemoryAccess,
    _.value := upstream.reg.aluResult
  )

  io.dataRequest.set(
    _.valid := upstream.reg.withSideEffects.hasMemoryAccess && !downstream.flowControl.stall && !downstream.flowControl.flush,
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
    _.cause := cause,
    _.memoryOperation := upstream.reg.memoryOperation,
    _.withSideEffects.set(
      _.exception := exception,
      _.isLoad := upstream.reg.withSideEffects.isLoad,
      _.isEcall := upstream.reg.withSideEffects.isEcall,
      _.isMret := upstream.reg.withSideEffects.isMret,
      _.writeCsrFile := upstream.reg.withSideEffects.isCsrWrite,
      _.writeRegisterFile := upstream.reg.withSideEffects.hasRegisterWriteBack,
      _.isBubble := upstream.reg.withSideEffects.isBubble,
      _.jumped := upstream.reg.withSideEffects.jump,
      _.hasMemoryAccess := upstream.reg.withSideEffects.hasMemoryAccess
    )
  )

  when(memNotReady || downstream.flowControl.flush) {
    downstream.reg.withSideEffects.set(
      _.exception := 0.B,
      _.isLoad := 0.B,
      _.isEcall := 0.B,
      _.isMret := 0.B,
      _.writeCsrFile := 0.B,
      _.writeRegisterFile := 0.B,
      _.isBubble := 1.B,
      _.jumped := 0.B,
      _.hasMemoryAccess := 0.B
    )
  }

}

