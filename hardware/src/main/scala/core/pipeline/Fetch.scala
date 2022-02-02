package core.pipeline

import chisel3._
import core.PipelineInterfaces.{DecodeToExecute, ExecuteToMemory, FetchToDecode, MemoryToWriteBack}
import core.PipelineStage

class Fetch extends PipelineStage(Bool(), new FetchToDecode) {



}


class Decode extends PipelineStage(new FetchToDecode, new DecodeToExecute) {

  val io = IO(new Bundle {
    val registerFileWrite = Input(new IntegerRegisterFile.WritePort)
  })

}

class Execute extends PipelineStage(new DecodeToExecute, new ExecuteToMemory) {



}

class Memory extends PipelineStage(new ExecuteToMemory, new MemoryToWriteBack) {



}

class WriteBack extends PipelineStage(new MemoryToWriteBack, new IntegerRegisterFile.WritePort) {



}