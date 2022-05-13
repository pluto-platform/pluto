package cores.nix

import cache.Cache
import cache.instruction.FillFsm
import charon.Tilelink
import chisel3._
import cores.PlutoCore
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryOperation}
import lib.util.{BundleItemAssignment, ByteSplitter, SeqConcat, SeqToVecMethods}

class Nix extends PlutoCore {

  val pipeline = Module(new Pipeline())

  pipeline.io.interrupts.custom := io.customInterrupts
  pipeline.io.interrupts.set(
    _.global := 0.B,
    _.previousGlobal := 0.B,
    _.external := io.externalInterrupt,
    _.timer := io.timerInterrupt,
    _.software := 0.B
  )

  val iCacheDim = Cache.Dimension(1024, 32  , BigInt(0) until BigInt(0x10000))
  val instructionCache = Module(new cache.instruction.DirectMapped(iCacheDim))
  instructionCache.io.request.valid := pipeline.io.instructionChannel.request.valid
  instructionCache.io.request.bits.address := pipeline.io.instructionChannel.request.bits.address
  pipeline.io.instructionChannel.request.ready := instructionCache.io.request.ready

  pipeline.io.instructionChannel.response.valid := instructionCache.io.response.valid
  pipeline.io.instructionChannel.response.bits.instruction := instructionCache.io.response.bits.instruction

  val fillFSM = Module(new FillFsm(iCacheDim))
  fillFSM.io.fillreq <> instructionCache.io.fillPort
  fillFSM.io.tilelink <> io.instructionRequester


  io.dataRequester.a.bits.set(
    _.opcode := Mux(pipeline.io.dataChannel.request.bits.op === MemoryOperation.Read, Tilelink.Operation.Get, Tilelink.Operation.PutPartialData),
    _.param := 0.U,
    _.size := pipeline.io.dataChannel.request.bits.accessWidth.asUInt,
    _.source := 0.U,
    _.address := pipeline.io.dataChannel.request.bits.address,
    _.mask := Seq.fill(4)(1.B).toVec,
    _.data := pipeline.io.dataChannel.request.bits.writeData.toBytes(4),
    _.corrupt := 0.B
  )
  io.dataRequester.a.valid := pipeline.io.dataChannel.request.valid
  pipeline.io.dataChannel.request.ready := io.dataRequester.d.ready

  pipeline.io.dataChannel.response.bits.set(
    _.readData := io.dataRequester.d.bits.data.concat,
    _.result := MemoryAccessResult.Success
  )
  io.dataRequester.d.ready := pipeline.io.dataChannel.response.ready
  pipeline.io.dataChannel.response.valid := io.dataRequester.d.valid

  io.pc := pipeline.io.pc

}
