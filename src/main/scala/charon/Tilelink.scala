package charon

import Chisel.DecoupledIO
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

  case class LinkParameters(
                              w: Int, // Width of the data bus in bytes
                              a: Int, // Width of each address field in bits
                              z: Int, // Width of each size field in bits
                              o: Int, // Number of bits to disambiguate per-link master sources
                              i: Int  // Number of bits to disambiguate per-link slaves
                              )

  abstract class Channel extends DecoupledIO(new Bundle {}) {

  }
  object Channel {
    class A(implicit params: Tilelink.LinkParameters) extends Channel {
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

    class D(implicit params: Tilelink.LinkParameters) extends Channel {
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

  abstract class Interface extends Bundle {

  }
  object Interface {
    class Manager(implicit params: Tilelink.LinkParameters) extends Interface {
      val a = new Channel.A
      val d = Flipped(new Channel.D)
    }
    class Client(implicit params: Tilelink.LinkParameters) extends Interface {
      val a = Flipped(new Channel.A)
      val d = new Channel.D
    }
  }



}
