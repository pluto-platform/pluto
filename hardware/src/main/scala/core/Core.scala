package core

import chisel3._

object Core {


  class CoreIO extends Bundle {
    // contains memory bus
    // make direction as seen from outside
  }

}

class Core extends Module {

  val io = IO(Flipped(new Core.CoreIO))

}
