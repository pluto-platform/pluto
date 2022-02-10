package core.pipeline.stages

import chisel3._

class PreDecode extends PipelineStage(new FetchToPreDecode, new PreDecodeToDecode) {

  val io = IO(new Bundle {
    val cacheResponse = Flipped(new InstructionCache.ResponsePort)
    val registerFileSource = Output(Vec(2, UInt(5.W)))
  })

}
