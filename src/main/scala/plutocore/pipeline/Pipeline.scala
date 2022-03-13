package plutocore.pipeline

import chisel3._
import chisel3.util.{DecoupledIO, ValidIO}
import lib.Interfaces.Channel
import lib.util.{BundleItemAssignment, FieldOptionExtractor}
import ControlTypes.{MemoryAccessResult, MemoryAccessWidth, MemoryOperation}
import PipelineInterfaces.{DecodeToExecute, ExecuteToMemory, FetchToDecode, MemoryToWriteBack}
import plutocore.pipeline.stages._
import plutocore.PipelineRegister


object Pipeline {

  class PipelineIO extends Bundle {
    val instructionChannel = new InstructionChannel
    val dataChannel = new DataChannel
  }
  class PipelineSimulationIO extends Bundle {
    val pc = UInt(32.W)
    val registerFile = Vec(32, UInt(32.W))
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


  case class State(
                  pc: BigInt,
                  registerFile: Seq[BigInt]
                  )



}


class Pipeline(sim: Option[Pipeline.State] = None) extends Module {

  val io = IO(new Pipeline.PipelineIO)
  val simulation = if(sim.isDefined) Some(IO(Output(new Pipeline.PipelineSimulationIO))) else None


  object Stage {
    val fetch = Module(new Fetch)
      .suggestName("stage_fetch")
    val decode = Module(new Decode)
      .suggestName("stage_decode")
    val execute = Module(new Execute)
      .suggestName("stage_execute")
    val memory = Module(new Memory)
      .suggestName("stage_memory")
    val writeBack = Module(new WriteBack)
      .suggestName("stage_writeback")
  }
  object StageReg {
    val fetch = PipelineRegister(new FetchToDecode)
      .suggestName("reg_fetch_decode")
    val decode = PipelineRegister(new DecodeToExecute)
      .suggestName("reg_decode_execute")
    val execute = PipelineRegister(new ExecuteToMemory)
      .suggestName("reg_execute_memory")
    val memory = PipelineRegister(new MemoryToWriteBack)
      .suggestName("reg_memory_writeback")
  }
  object Components {
    val pc = Module(new ProgramCounter(sim.getFieldOption(_.pc)))
      .suggestName("pc")
    val registerFile = Module(new IntegerRegisterFile(sim.getFieldOption(_.registerFile)))
      .suggestName("registerfile")
    val forwader = Module(new Forwarder)
      .suggestName("forwarder")
    val hazardDetector = Module(new HazardDetector)
      .suggestName("hazard_detector")
    val csrFile = Module(new ControlAndStatusRegisterFile)
      .suggestName("csrfile")
    val branchingUnit = Module(new BranchingUnit)
      .suggestName("branching_unit")
    val branchPredictor = Module(new SimpleBranchPredictor)
      .suggestName("branch_predictor")
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
  Stage.fetch.upstream.data.pc := Components.pc.io.value

  io.dataChannel.set(
    _.request <> Stage.memory.io.dataRequest,
    _.response <> Stage.writeBack.io.dataResponse
  )
  io.instructionChannel.set(
    _.request <> Components.pc.io.instructionRequest,
    _.response <> Stage.fetch.io.instructionResponse
  )

  Components.pc.io.set(
    _.stall := Stage.fetch.upstream.flowControl.stall || !io.instructionChannel.request.ready
  )
  Components.registerFile.io.set(
    _.source.request <> Stage.fetch.io.registerSources,
    _.source.response <> Stage.decode.io.registerSources,
    _.write <> Stage.writeBack.io.registerFile
  )
  Components.forwader.io.set(
    _.decode <> Stage.decode.io.forwarding,
    _.execute <> Stage.execute.io.forwarding,
    _.memory <> Stage.memory.io.forwarding,
    _.writeBack <> Stage.writeBack.io.forwarding
  )
  Components.hazardDetector.io.set(
    _.decode <> Stage.decode.io.hazardDetection,
    _.execute <> Stage.execute.io.hazardDetection,
    _.memory <> Stage.memory.io.hazardDetection
  )
  Components.csrFile.io.set(
    _.readRequest <> Stage.execute.io.csrRequest,
    _.readResponse <> Stage.memory.io.csrResponse,
    _.writeRequest <> Stage.writeBack.io.csrFile
  )
  Components.branchingUnit.io.set(
    _.fetch <> Stage.fetch.io.branching,
    _.decode <> Stage.decode.io.branching,
    _.pc <> Components.pc.io.branching,
    _.predictor <> Components.branchPredictor.io
  )

  if(sim.isDefined) {
    simulation.get.pc := Components.pc.io.value
    simulation.get.registerFile := Components.registerFile.simulation.get.registers
  }


}

object PipelineEmitter extends App {
  emitVerilog(new Pipeline)
}