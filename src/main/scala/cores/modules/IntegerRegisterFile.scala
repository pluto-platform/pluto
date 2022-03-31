package cores.modules

import chisel3._
import chisel3.util.Valid

object IntegerRegisterFile {

  class SourceRequest extends Bundle {
    val index = Vec(2,UInt(5.W))
  }
  class SourceResponse extends Bundle {
    val data = Vec(2, UInt(32.W))
  }

  class WriteRequest extends Bundle {
    val index = UInt(5.W)
    val data = UInt(32.W)
  }

  case class SimulationSetup(init: Seq[UInt])

  class SimulationSignals extends Bundle {
    val registers = Output(Vec(32, UInt(32.W)))
  }

}

class IntegerRegisterFile(init: Option[Seq[BigInt]] = None) extends Module {

  if(init.isDefined) require(init.get.length == 32)

  val io = IO(new Bundle {

    val source = new Bundle {
      val request = Input(new IntegerRegisterFile.SourceRequest)
      val response = Output(new IntegerRegisterFile.SourceResponse)
    }
    val write = Flipped(Valid(new IntegerRegisterFile.WriteRequest))

  })
  val simulation = if(init.isDefined) Some(IO(new IntegerRegisterFile.SimulationSignals)) else None

  init match {
    case Some(init) =>

      val memory = RegInit(VecInit(init.map(_.U(32.W))))

      io.source.request.index.zip(io.source.response.data).foreach { case (address, data) =>
        data := memory(RegNext(address))
      }
      when(io.write.valid) {
        memory(io.write.bits.index) := io.write.bits.data
      }

      simulation.get.registers := memory

    case None =>

      val memory = SyncReadMem(32, UInt(32.W))

      // handle both read requests
      io.source.request.index.zip(io.source.response.data).foreach { case (address, data) =>
        data := memory.read(address)
      }

      // handle write request
      when(io.write.valid) {
        memory.write(io.write.bits.index, io.write.bits.data)
      }

  }

}


