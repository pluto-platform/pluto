package charon

import chisel3._
import chisel3.experimental.ChiselEnum

object Tilelink {

  object Operation extends ChiselEnum {
    val PutFullData = Value(0.U(3.W))
    val PutPartialData = Value(1.U(3.W))
    val Get = Value(4.U(3.W))
  }
  object Response extends ChiselEnum {
    val AccessAck = Value(0.U(3.W))
    val AccessAckData = Value(1.U(3.W))
  }

  // (address & (2^size -1) == 0) always holds

  case class PerLinkParameters(
                              w: Int, // Width of the data bus in bytes
                              a: Int, // Width of each address field in bits
                              z: Int, // Width of each size field in bits
                              o: Int, // Number of bits to disambiguate per-link master sources
                              i: Int  // Number of bits to disambiguate per-link slaves
                              )

  abstract class Channel extends Bundle {
    val valid = Bool()
  }
  object Channel {
    class A(params: Tilelink.PerLinkParameters) extends Bundle {
      import params._
      val opcode = Tilelink.Operation()
      val param = UInt(3.W)
      val size = UInt(z.W)
      val source = UInt(o.W)
      val address = UInt(a.W)
      val mask = Vec(w, Bool())
      val data = Vec(w,UInt(8.W))
      val corrupt = Bool()
    }

    class D(params: Tilelink.PerLinkParameters) extends Bundle {
      import params._
      val opcode = Tilelink.Response()
      val param = UInt(2.W)
      val size = UInt(z.W)
      val source = UInt(o.W)
      val sink = UInt(i.W)
      val denied = Bool()
      val data = Vec(w,UInt(8.W))
      val corrupt = Bool()
    }
  }


}
