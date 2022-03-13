package core.pipeline

import chisel3._
import core.Forwarding
import lib.util.Delay

class Forwarder extends Module {

  val io = IO(new Bundle {
    val decode = Flipped(new Forwarding.DecodeChannel)
    val execute = Flipped(new Forwarding.ExecuteChannel)
    val memory = Flipped(new Forwarding.MemoryChannel)
    val writeBack = Flipped(new Forwarding.WriteBackChannel)
  })

/*
need to know whether stall occurs and set forwarding source accordingly

- instruction ahead of branch changes either source register -> stall in decode -> forward from memory to decode
- instruction ahead of jalr changes s0 -> stall in decode -> forward from memory to decode
- instruction ahead is a load to either source register -> stall in decode -> forward from writeBack to execute
- instruction ahead changes either source register -> forward from memory to execute
- instruction two ahead changes either source register -> forward from memory to decode -> unless stall due to other channel -> forward from wb to dec
 */



  (io.decode.channel,io.execute.channel)
    .zipped.toList
    .foreach { case (decodeChannel, executeChannel) =>
      val memoryToDecode = decodeChannel.source === io.memory.destination && io.memory.canForward
      val writeBackToDecode = decodeChannel.source === io.writeBack.destination && io.writeBack.canForward
      val memoryToExecute = executeChannel.source === io.memory.destination && io.memory.canForward
      val writeBackToExecute = executeChannel.source === io.writeBack.destination && io.writeBack.canForward
      decodeChannel.shouldForward := (memoryToDecode || writeBackToDecode)
      decodeChannel.value := Mux(memoryToDecode, io.memory.value, io.writeBack.value)
      executeChannel.shouldForward := (memoryToExecute || writeBackToExecute)
      executeChannel.value := Mux(memoryToExecute, io.memory.value, io.writeBack.value)
  }

}
