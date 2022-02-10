package core
import chisel3._
import chisel3.util.{DecoupledIO, ValidIO}
import core.ControlTypes.{MemoryAccessResult, MemoryAccessWidth, MemoryOperation}
import core.pipeline.IntegerRegisterFile
import core.pipeline.stages.{Decode, Execute, Fetch, Memory, PreDecode, WriteBack}
import lib.Interfaces.Channel


object Pipeline {

  class PipelineIO extends Bundle {
    val instructionChannel = new InstructionChannel
    val dataChannel = new DataChannel
  }



  object InstructionChannel {
    class Request extends DecoupledIO(new Bundle {
      val address = UInt(32.W)
    })
    class Response extends ValidIO(new Bundle {
      val instruction = UInt(32.W)
    })
  }
  class InstructionChannel extends Channel(new InstructionChannel.Request, new InstructionChannel.Response)

  object DataChannel {
    class Request extends DecoupledIO(new Bundle {
      val address = UInt(32.W)
      val writeData = UInt(32.W)
      val op = MemoryOperation()
      val accessWidth = MemoryAccessWidth()
    })
    class Response extends ValidIO(new Bundle {
      val readData = UInt(32.W)
      val result = MemoryAccessResult()
    })
  }
  class DataChannel extends Channel(new DataChannel.Request, new DataChannel.Response)

}

class Pipeline extends Module {

  val io = IO(new Pipeline.PipelineIO)

  object Stage {
    val fetch = Module(new Fetch)
    val preDecode = Module(new PreDecode)
    val decode = Module(new Decode)
    val execute = Module(new Execute)
    val memory = Module(new Memory)
    val writeBack = Module(new WriteBack)
  }
  object Modules {
    val registerFile = Module(new IntegerRegisterFile)
  }



}
