package core
import chisel3._
import core.pipeline.{Decode, Execute, Fetch, Memory, WriteBack}

class Pipeline extends {

  object Stage {
    val fetch = Module(new Fetch)
    val decode = Module(new Decode)
    val execute = Module(new Execute)
    val memory = Module(new Memory)
    val writeBack = Module(new WriteBack)
  }

  Stage.fetch :> Stage.decode :> Stage.execute :> Stage.memory :> Stage.writeBack

  Stage.decode.io.registerFileWrite := Stage.writeBack.out

}
