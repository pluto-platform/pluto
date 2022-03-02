package core
import chisel3._
import chisel3.util.{DecoupledIO, ValidIO}
import core.ControlTypes.{MemoryAccessResult, MemoryAccessWidth, MemoryOperation}
import core.PipelineInterfaces.{DecodeToExecute, ExecuteToMemory, FetchToDecode, MemoryToWriteBack}
import core.pipeline.{BranchingUnit, ControlAndStatusRegisterFile, Forwarder, IntegerRegisterFile, LoadUseHazardDetector, ProgramCounter, SimpleBranchPredictor}
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
    val fetch = Module(new Fetch).suggestName("stage_fetch")
    val decode = Module(new Decode).suggestName("stage_decode")
    val execute = Module(new Execute).suggestName("stage_execute")
    val memory = Module(new Memory).suggestName("stage_memory")
    val writeBack = Module(new WriteBack).suggestName("stage_writeback")
  }
  object StageReg {
    val fetch = PipelineRegister(new FetchToDecode).suggestName("reg_fetch_decode")
    val decode = PipelineRegister(new DecodeToExecute).suggestName("reg_decode_execute")
    val execute = PipelineRegister(new ExecuteToMemory).suggestName("reg_execute_memory")
    val memory = PipelineRegister(new MemoryToWriteBack).suggestName("reg_memory_writeback")
  }
  object hello {
    val pc = Module(new ProgramCounter).suggestName("pc")
    val registerFile = Module(new IntegerRegisterFile).suggestName("registerfile")
    val forwader = Module(new Forwarder).suggestName("forwarder")
    val loadUseHazardDetector = Module(new LoadUseHazardDetector).suggestName("load_use_hazard_detector")
    val csrFile = Module(new ControlAndStatusRegisterFile).suggestName("csrfile")
    val branchingUnit = Module(new BranchingUnit).suggestName("branching_unit")
    val branchPredictor = Module(new SimpleBranchPredictor).suggestName("branch_predictor")
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
  Stage.fetch.upstream.data.pc := hello.pc.io.value

  io.dataChannel.set(
    _.request <> Stage.memory.io.dataRequest,
    _.response <> Stage.writeBack.io.dataResponse
  )
  io.instructionChannel.set(
    _.request <> hello.pc.io.instructionRequest,
    _.response <> Stage.fetch.io.instructionResponse
  )

  hello.pc.io.set(
    _.stall := Stage.fetch.upstream.flowControl.stall || !io.instructionChannel.request.ready
  )
  hello.registerFile.io.set(
    _.source.request <> Stage.fetch.io.registerSources,
    _.source.response <> Stage.decode.io.registerSources,
    _.write <> Stage.writeBack.io.registerFile
  )
  hello.forwader.io.set(
    _.execute <> Stage.execute.io.forwarding,
    _.memory <> Stage.memory.io.forwarding,
    _.writeBack <> Stage.writeBack.io.forwarding
  )
  hello.loadUseHazardDetector.io.set(
    _.decode <> Stage.decode.io.loadUseHazard,
    _.execute <> Stage.execute.io.loadUseHazard
  )
  hello.csrFile.io.set(
    _.readRequest <> Stage.execute.io.csrRequest,
    _.readResponse <> Stage.memory.io.csrResponse,
    _.writeRequest <> Stage.writeBack.io.csrFile
  )
  hello.branchingUnit.io.set(
    _.fetch <> Stage.fetch.io.branching,
    _.decode <> Stage.decode.io.branching,
    _.execute <> Stage.execute.io.branching,
    _.pc <> hello.pc.io.branching,
    _.predictor <> hello.branchPredictor.io
  )

}

object PipelineEmitter extends App {
  emitVerilog(new Pipeline)
}