import chisel3._
import chisel3.experimental.ChiselEnum

object Opcodes extends ChiselEnum {
  val load = Value(0x03.U) // I-type | 0x03
  val miscMem = Value(0x0F.U) // I-type | 0x0F
  val imm = Value(0x13.U) // I-type | 0x13
  val auipc = Value(0x17.U) // U-type | 0x17
}
