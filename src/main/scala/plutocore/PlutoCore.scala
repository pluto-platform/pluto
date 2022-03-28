package plutocore

import chisel3._
import plutocore.branchpredictor.{BranchPredictor, LoopBranchPredictor}
import plutocore.cache.data.DataCache
import plutocore.cache.instruction.InstructionCache

object PlutoCore {


  class CoreIO extends Bundle {
    // contains memory bus
    // make direction as seen from outside
  }

}

class PlutoCore(
               instructionCache : Option[() => InstructionCache],
               dataCache        : Option[() => DataCache],
               branchPredictor  : => BranchPredictor = new LoopBranchPredictor
               ) extends Module {

  val io = IO(Flipped(new PlutoCore.CoreIO))

}
