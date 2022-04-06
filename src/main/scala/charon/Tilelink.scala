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

  case class Parameters(
                              w: Int, // Width of the data bus in bytes
                              a: Int, // Width of each address field in bits
                              z: Int, // Width of each size field in bits
                              o: Int, // Number of bits to disambiguate per-link master sources
                              i: Int  // Number of bits to disambiguate per-link slaves
                              )

  abstract class Channel extends DecoupledIO(new Bundle {}) {

  }
  object Channel {
    class A(implicit params: Tilelink.Parameters) extends Channel {
      import params._
      val opcode = Tilelink.Operation()
      val param = UInt(3.W)
      val size = UInt(z.W)
      val source = UInt(o.W)
      val address = UInt(a.W)
      val mask = Vec(w, Bool())
      val data = Vec(w,UInt(8.W))
      val corrupt = Bool()

      def filterByRange(range: AddressRange): Tilelink.Channel.A = {
        val a = Wire(new Tilelink.Channel.A)
        a <> this
        a.valid := range.contains(address) && valid
        a
      }
    }

    class D(implicit params: Tilelink.Parameters) extends Channel {
      import params._
      val opcode = Tilelink.Response()
      val param = UInt(2.W)
      val size = UInt(z.W)
      val source = UInt(o.W)
      val sink = UInt(i.W)
      val denied = Bool()
      val data = Vec(w,UInt(8.W))
      val corrupt = Bool()

      def filterBySource(id: UInt): D = {
        val d = Wire(new Tilelink.Channel.D)
        d <> this
        d.valid := source === id && valid
        d
      }
    }
  }

  abstract class Interface extends Bundle {

  }
  object Agent {
    object Interface {
      class Requester(implicit params: Tilelink.Parameters) extends Interface {
        val a = new Channel.A
        val d = Flipped(new Channel.D)
      }
      class Responder(val size: Int = 0)(implicit params: Tilelink.Parameters) extends Interface {
        val a = Flipped(new Channel.A)
        val d = new Channel.D
      }
    }
  }



  trait Agent {

    val io: Bundle

    val requesterInterfaces = io.elements.collect {
      case (_, req: Agent.Interface.Requester) => req
      case (_, reqVec: Vec[Agent.Interface.Requester]) => reqVec
    }.toSeq
    val responderInterfaces = io.elements.collect {
      case (_, res: Agent.Interface.Responder) => res
      case (_, resVec: Vec[Agent.Interface.Responder]) => resVec
    }.toSeq

  }



}
