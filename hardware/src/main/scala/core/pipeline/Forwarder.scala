package core.pipeline

import chisel3._
import core.Forwarding
import lib.util.BundleItemAssignment

class Forwarder extends Module {

  val io = IO(new Bundle {
    val execute = Flipped(new Forwarding.ExecuteChannel)
    val memory = Flipped(new Forwarding.ProviderChannel)
    val writeBack = Flipped(new Forwarding.ProviderChannel)
  })

  val matchInMem = io.execute.source.map(_ === io.memory.destination && io.memory.destinationIsNonZero)
  val matchInWb = io.execute.source.map(_ === io.writeBack.destination && io.writeBack.destinationIsNonZero)

  io.execute.set(
    _.value := Mux(matchInWb(0) || matchInMem(1), io.memory.value, io.writeBack.value),
    _.shouldForward.zip(matchInMem.zip(matchInWb)).foreach { case (fwd,(mem,wb)) => fwd := mem || wb}
  )

}
