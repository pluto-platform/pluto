package cores

import cache.data.DataCache
import cache.instruction.InstructionCache
import charon.Tilelink
import chisel3._
import cores.modules.branchpredictor.{BranchPredictor, LoopBranchPredictor}

object PlutoCore {

  case class Configuration(
                            instructionCache : Option[InstructionCache],
                            dataCache        : Option[DataCache],
                            branchPredictor  : BranchPredictor = new LoopBranchPredictor
                          )

  class CoreIO extends Bundle {
    val customInterrupts = Input(Vec(16, Bool()))
    val externalInterrupt = Input(Bool())
    val timerInterrupt = Input(Bool())
    val instructionRequester = Tilelink.Agent.Interface.Requester(Tilelink.Parameters(4, 32, 2, Some(5), Some(5)))
    val dataRequester = Tilelink.Agent.Interface.Requester(Tilelink.Parameters(4, 32, 2, Some(5), Some(5)))
    val pc = Output(UInt(32.W))
  }

}

abstract class PlutoCore extends Module {

  val io = IO(new PlutoCore.CoreIO)

}
