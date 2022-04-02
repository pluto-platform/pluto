package cores.nix

import chisel3._
import cores.modules.{ControlAndStatusRegisterFile, IntegerRegisterFile, PipelineControl, PipelineRegister}
import chisel3.util.{DecoupledIO, ValidIO}
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryAccessWidth, MemoryOperation}
import cores.lib.riscv.Opcode
import cores.nix.stages.{Decode, Execute, Fetch, Memory, WriteBack}
import cores.nix.stages.Interfaces.{DecodeToExecute, ExecuteToMemory, FetchToDecode, MemoryToWriteBack}
import lib.Interfaces.Channel
import lib.util.{BundleItemAssignment, Delay, FieldOptionExtractor}


object Pipeline {

  class PipelineIO extends Bundle {
    val instructionChannel = new InstructionChannel
    val dataChannel = new DataChannel
  }
  class PipelineSimulationIO extends Bundle {
    val pc = UInt(32.W)
    val registerFile = Vec(32, UInt(32.W))
    val isEcall = Bool()
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


class Pipeline(state: Option[Pipeline.State] = None) extends Module {

  val io = IO(new Pipeline.PipelineIO)
  val simulation = if(state.isDefined) Some(IO(Output(new Pipeline.PipelineSimulationIO))) else None


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
  object Components {
    val pc = Module(new ProgramCounter(state.getFieldOption(_.pc)))
    val registerFile = Module(new IntegerRegisterFile(state.getFieldOption(_.registerFile)))
    val forwader = Module(new Forwarder)
    val hazardDetector = Module(new HazardDetector)
    val csrFile = Module(new ControlAndStatusRegisterFile)
    val exceptionUnit = Module(new ExceptionUnit)
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
    _.execute <> Stage.execute.io.forwarding,
    _.memory <> Stage.memory.io.forwarding,
    _.writeBack <> Stage.writeBack.io.forwarding
  )
  Components.hazardDetector.io.set(
    _.decode <> Stage.decode.io.hazardDetection,
    _.execute <> Stage.execute.io.hazardDetection,
  )
  Components.csrFile.io.set(
    _.readRequest <> Stage.execute.io.csrRequest,
    _.readResponse <> Stage.memory.io.csrResponse,
    _.writeRequest <> Stage.writeBack.io.csrFile
  )
  Components.pc.io.branching <> Stage.memory.io.branching
  Components.csrFile.io.instructionRetired := Stage.writeBack.io.instructionRetired
  Components.exceptionUnit.io.set(
    _.decode <> Stage.decode.io.exception,
    _.writeBack <> Stage.writeBack.io.exception,
    _.csr <> Components.csrFile.io.exceptionUnit,
    _.programCounter <> Components.pc.io.exception
  )

  if(state.isDefined) {
    simulation.get.pc := Components.pc.io.value
    simulation.get.registerFile := Components.registerFile.simulation.get.registers
    simulation.get.isEcall := Stage.writeBack.io.ecallRetired
  }


}

/*
- block all csr instructions when one is in the pipeline
  - use `isCsrAccess`

- decode should produce alufun and memaccesswidth again since alufun should always be add for mem stuff
- alufun has to be zero for jal (jalr has funct3 0 and should be fine)
- auipc needs alufun 0
- lui needs passthrough -> we have got pc+4 in decode (need channel from fetch) -> pass it
 */