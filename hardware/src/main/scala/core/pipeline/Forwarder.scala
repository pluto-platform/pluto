package core.pipeline

import chisel3._
import core.Forwarding
import lib.util.{BoolVec,  Delay}

class Forwarder extends Module {

  val io = IO(new Bundle {
    val fetch = Flipped(new Forwarding.FetchChannel)
    val decode = Flipped(new Forwarding.DecodeChannel)
    val execute = Flipped(new Forwarding.ExecuteChannel)
    val memory = Flipped(new Forwarding.MemoryChannel)
    val writeBack = Flipped(new Forwarding.WriteBackChannel)
  })


  // collect destination and source matches
  object Match {
    val Seq(decode,execute,memory) =
      Seq(io.decode.nextExecuteInfo, io.execute.nextMemoryInfo, io.memory.nextWriteBackInfo)
        .map { info => io.fetch.source.map(_ === info.destination && info.canForward) }
  }


  io.decode.shouldForward(0) :=
    Delay(Match.decode(0) && io.fetch.needsValuesInDecode, cycles = 2) ||
      Delay(Match.execute(0) || Match.memory(0), cycles = 1)
  io.decode.shouldForward(1) :=
    Delay(Match.decode(1) && io.fetch.needsValuesInDecode, cycles = 2) ||
      Delay(Match.execute(1) || Match.memory(1), cycles = 1)

  io.decode.forwardedValue := Mux(
    Delay(Match.memory.orR, cycles = 1),
    io.writeBack.writeBackValue,
    io.memory.writeBackValue
  )

  io.execute.shouldForward(0) :=
    Delay(Match.decode(0) && !io.fetch.needsValuesInDecode, cycles = 2)
  io.execute.shouldForward(1) :=
    Delay(Match.decode(1) && !io.fetch.needsValuesInDecode, cycles = 2)

  io.execute.forwardedValue := io.memory.writeBackValue

}
