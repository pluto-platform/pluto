package core
import chisel3._
import chisel3.util.{DecoupledIO, ValidIO}
import core.ControlTypes.{MemoryAccessResult, MemoryAccessWidth, MemoryOperation}
import core.PipelineInterfaces.{DecodeToExecute, ExecuteToMemory, FetchToDecode, MemoryToWriteBack}
import core.pipeline.{ControlAndStatusRegisterFile, Forwarder, IntegerRegisterFile, LoadUseHazardDetector, ProgramCounter}
import core.pipeline.stages.{Decode, Execute, Fetch, Memory, WriteBack}
import lib.Interfaces.Channel
import lib.util.BundleItemAssignment


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
    val decode = Module(new Decode)
    val execute = Module(new Execute)
    val memory = Module(new Memory)
    val writeBack = Module(new WriteBack)
  }
  object StageReg {
    val fetch = PipelineRegister(new FetchToDecode)
    val decode = PipelineRegister(new DecodeToExecute)
    val execute = PipelineRegister(new ExecuteToMemory)
    val memory = PipelineRegister(new MemoryToWriteBack)
  }
  object Modules {
    val pc = Module(new ProgramCounter)
    val registerFile = Module(new IntegerRegisterFile)
    val forwader = Module(new Forwarder)
    val loadUseHazardDetector = Module(new LoadUseHazardDetector)
    val csrFile = Module(new ControlAndStatusRegisterFile)
  }

  Stage.fetch
    .attachRegister(StageReg.fetch)
    .attachStage(Stage.decode)
    .attachRegister(StageReg.decode)
    .attachStage(Stage.execute)
    .attachRegister(StageReg.execute)
    .attachStage(Stage.memory)
    .attachRegister(StageReg.memory)
    .attachStage(Stage.writeBack)

  Stage.writeBack.downstream.flowControl := 0.U.asTypeOf(new PipelineControl)
  Stage.fetch.upstream.data.pc := Modules.pc.io.value

  io.dataChannel.set(
    _.request <> Stage.memory.io.dataRequest,
    _.response <> Stage.writeBack.io.dataResponse
  )
  io.instructionChannel.set(
    _.request <> Modules.pc.io.instructionRequest,
    _.response <> Stage.fetch.io.instructionResponse
  )

  Modules.registerFile.io.set(
    _.source.request <> Stage.fetch.io.registerSources,
    _.source.response <> Stage.decode.io.registerSources,
    _.write <> Stage.writeBack.io.registerFile
  )
  Modules.forwader.io.set(
    _.execute <> Stage.execute.io.forwarding,
    _.memory <> Stage.memory.io.forwarding,
    _.writeBack <> Stage.writeBack.io.forwarding
  )
  Modules.loadUseHazardDetector.io.set(
    _.decode <> Stage.decode.io.loadUseHazard,
    _.execute <> Stage.execute.io.loadUseHazard
  )
  Modules.csrFile.io.set(
    _.readRequest <> Stage.execute.io.csrRequest,
    _.readResponse <> Stage.memory.io.csrResponse,
    _.writeRequest <> Stage.writeBack.io.csrFile
  )

}

object PipelineEmitter extends App {
  emitVerilog(new Pipeline)
}