package cores

import chisel3._
import cores.modules.branchpredictor.{BranchPredictor, LoopBranchPredictor}
import cores.modules.cache.data.DataCache
import cores.modules.cache.instruction.InstructionCache

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
