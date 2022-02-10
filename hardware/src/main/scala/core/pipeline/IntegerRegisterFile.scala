package core.pipeline

import chisel3._
import chisel3.stage.ChiselStage
import chisel3.util.{Valid, ValidIO}

object IntegerRegisterFile {

  class SourcePort extends Bundle {
    val address = Input(UInt(5.W))
    val data = Output(UInt(32.W))
  }

  class WritePort extends ValidIO(new Bundle {
    val address = Input(UInt(5.W))
    val data = Input(UInt(32.W))
  })

}

class IntegerRegisterFile extends Module {

  val io = IO(new Bundle {

    val source = Vec(2, new IntegerRegisterFile.SourcePort)
    val write = Flipped(new IntegerRegisterFile.WritePort)

  })

  val ram = SyncReadMem(32, UInt(32.W))

  val writeIsNotX0 = io.write.bits.address.orR

  io.source.foreach { source =>
    source.data := Mux(
      io.write.valid && writeIsNotX0 && source.address === io.write.bits.address,
      io.write.bits.data,
      ram.read(source.address)
    )
  }

  when(io.write.valid && writeIsNotX0) {
    ram.write(io.write.bits.address, io.write.bits.data)
  }

}