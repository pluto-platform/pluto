package core.pipeline.stages

import chisel3._
import core.PipelineInterfaces.{DecodeToExecute, PreDecodeToDecode}
import core.PipelineStage

class Decode extends PipelineStage(new PreDecodeToDecode, new DecodeToExecute) {

  val io = IO(new Bundle {
  })

}