package cores.nix

import chisel3._

object Forwarding {
  class ForwardingChannel extends Bundle {
    val source = Output(UInt(5.W))
    val shouldForward = Input(Bool())
    val value = Input(UInt(32.W))
  }
  class ExecuteChannel extends Bundle {
    val channel = Vec(2, new ForwardingChannel)
  }
  class MemoryChannel extends Bundle {
    val destination = Output(UInt(5.W))
    val canForward = Output(Bool())
    val value = Output(UInt(32.W))
  }
  class WriteBackChannel extends Bundle {
    val destination = Output(UInt(5.W))
    val canForward = Output(Bool())
    val value = Output(UInt(32.W))
  }
}
class Forwarder extends Module {

  val io = IO(new Bundle {
    val execute = Flipped(new Forwarding.ExecuteChannel)
    val memory = Flipped(new Forwarding.MemoryChannel)
    val writeBack = Flipped(new Forwarding.WriteBackChannel)
  })

  io.execute.channel
    .foreach { channel =>
      val memoryToExecute = channel.source === io.memory.destination && io.memory.canForward
      val writeBackToExecute = channel.source === io.writeBack.destination && io.writeBack.canForward
      channel.shouldForward := (memoryToExecute || writeBackToExecute)
      channel.value := Mux(memoryToExecute, io.memory.value, io.writeBack.value)
  }

}
