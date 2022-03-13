package plutocore

import chisel3._

object PlutoCore {


  class CoreIO extends Bundle {
    // contains memory bus
    // make direction as seen from outside
  }

}

class PlutoCore extends Module {

  val io = IO(Flipped(new PlutoCore.CoreIO))

}
