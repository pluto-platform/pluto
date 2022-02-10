package core.pipeline.stages

import chisel3._
import core.PipelineInterfaces.MemoryToWriteBack
import core.PipelineStage

class WriteBack extends PipelineStage(new MemoryToWriteBack, new Bundle {}) {



}