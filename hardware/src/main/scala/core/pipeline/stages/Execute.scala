package core.pipeline.stages

import chisel3._
import core.PipelineInterfaces.{DecodeToExecute, ExecuteToMemory}
import core.PipelineStage

class Execute extends PipelineStage(new DecodeToExecute, new ExecuteToMemory) {



}