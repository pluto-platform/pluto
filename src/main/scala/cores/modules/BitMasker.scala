package cores.modules

import chisel3._
import chisel3.experimental.ChiselEnum

import lib.LookUp._


object BitMasker {
  // funct3(1,0)
  object BitMaskerFunction extends ChiselEnum {
    val PassThrough = Value(1.U)
    val Set = Value(2.U)
    val Clear = Value(3.U)
    def fromFunct3(funct3: UInt): BitMaskerFunction.Type = BitMaskerFunction.safe(funct3(1,0))._1
  }
}

class BitMasker extends Module {
  import cores.modules.BitMasker.BitMaskerFunction
  import cores.modules.BitMasker.BitMaskerFunction._

  val io = IO(new Bundle {
    val operand = Input(Vec(2, UInt(32.W)))
    val result = Output(UInt(32.W))
    val function = Input(BitMaskerFunction())
  })

  io.result := lookUp(io.function) in (
    PassThrough -> io.operand(1),
    Set         -> (io.operand(0) | io.operand(1)),
    Clear       -> (io.operand(0) & (~io.operand(1)).asUInt)
  )

}
