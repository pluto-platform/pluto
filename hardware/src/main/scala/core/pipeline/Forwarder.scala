package core.pipeline

import chisel3._
import core.Forwarding
import lib.util.Delay

class Forwarder extends Module {

  val io = IO(new Bundle {
    val fetch = Flipped(new Forwarding.FetchChannel)
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

  (io.fetch.source,io.decode.channel,io.execute.channel)
    .zipped.toList
    .foreach { case (source, decodeChannel, executeChannel) =>
      val delayedMemoryToDecode = Delay(source.id === io.decode.nextExecuteInfo.destination && io.decode.nextExecuteInfo.canForward && source.neededInDecode, cycles = 2)
      val memoryToDecode = Delay(source.id === io.execute.nextMemoryInfo.destination && io.execute.nextMemoryInfo.canForward, cycles = 1)
      val memoryToExecute = Delay(source.id === io.decode.nextExecuteInfo.destination && io.decode.nextExecuteInfo.canForward && !source.neededInDecode, cycles = 2)
      val writeBackToDecode = Delay(source.id === io.memory.nextWriteBackInfo.destination && io.memory.nextWriteBackInfo.canForward, cycles = 1)
      decodeChannel.shouldForward := (delayedMemoryToDecode || memoryToDecode || writeBackToDecode)
      decodeChannel.value := Mux(delayedMemoryToDecode || memoryToDecode, io.memory.writeBackValue, io.writeBack.writeBackValue)
      executeChannel.shouldForward := memoryToExecute && source.acceptsForwardingInExecute
      executeChannel.value := io.memory.writeBackValue
  }

}
