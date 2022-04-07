package cores

import cache.data.DataCache
import cache.instruction.InstructionCache
import chisel3._
import cores.modules.branchpredictor.{BranchPredictor, LoopBranchPredictor}

object PlutoCore {

  case class Configuration(
                            instructionCache : Option[InstructionCache],
                            dataCache        : Option[DataCache],
                            branchPredictor  : BranchPredictor = new LoopBranchPredictor
                          )

  class CoreIO extends Bundle {

  }

}

abstract class PlutoCore extends Module {

  val io = IO(new PlutoCore.CoreIO)

}
