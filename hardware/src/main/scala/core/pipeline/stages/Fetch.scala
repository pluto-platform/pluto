package core.pipeline.stages

import chisel3._
import core.PipelineInterfaces._
import core.PipelineStage
import core.pipeline.{InstructionCache, IntegerRegisterFile}

class Fetch extends PipelineStage(Bool(), new FetchToPreDecode) {

  val io = IO(new Bundle {
    val cacheRequest = new InstructionCache.RequestPort
  })





}









