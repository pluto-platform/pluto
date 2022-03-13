package plutocore.pipeline

import chisel3._
import ControlTypes.BitMaskerFunction
import ControlTypes.BitMaskerFunction._
import lib.LookUp._

class BitMasker extends Module {

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
