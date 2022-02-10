package core.pipeline

import chisel3._
import core.PipelineInterfaces.{DecodeToExecute, ExecuteToMemory, FetchToPreDecode, MemoryToWriteBack, PreDecodeToDecode}
import core.PipelineStage

class Fetch extends PipelineStage(Bool(), new FetchToPreDecode) {

  val io = IO(new Bundle {
    val cacheRequest = new InstructionCache.RequestPort
  })





}


class PreDecode extends PipelineStage(new FetchToPreDecode, new PreDecodeToDecode) {

  val io = IO(new Bundle {
    val cacheResponse = Flipped(new InstructionCache.ResponsePort)
    val registerFileSource = Output(Vec(2, UInt(5.W)))
  })

}

class Execute extends PipelineStage(new DecodeToExecute, new ExecuteToMemory) {



}

class Memory extends PipelineStage(new ExecuteToMemory, new MemoryToWriteBack) {



}

class WriteBack extends PipelineStage(new MemoryToWriteBack, new IntegerRegisterFile.WritePort) {



}