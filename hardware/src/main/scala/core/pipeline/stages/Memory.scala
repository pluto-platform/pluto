package core.pipeline.stages

import chisel3._
import core.PipelineInterfaces.{ExecuteToMemory, MemoryToWriteBack}
import core.PipelineStage

class Memory extends PipelineStage(new ExecuteToMemory, new MemoryToWriteBack) {



}