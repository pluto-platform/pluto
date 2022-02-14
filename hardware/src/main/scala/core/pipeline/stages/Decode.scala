package core.pipeline.stages

import chisel3._
import core.PipelineInterfaces.{DecodeToExecute, FetchToDecode}
import core.PipelineStage
import lib.Immediates.FromInstructionToImmediate

class Decode extends PipelineStage(new FetchToDecode, new DecodeToExecute) {

  val io = IO(new Bundle {
  })


  upstream.instruction.extractImmediate.bType

}