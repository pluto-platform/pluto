package charon

import Chisel.DecoupledIO
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.Decoupled
import lib.LookUp.lookUp
import lib.util.InputOutputExtender
import lib.Types.Byte

object Tilelink {

  object Operation extends ChiselEnum {
    val PutFullData = Value(0.U(3.W))
    val PutPartialData = Value(1.U(3.W))
    val Get = Value(4.U(3.W))
  }
  object Response extends ChiselEnum {
    val AccessAck = Value(0.U(3.W))
    val AccessAckData = Value(1.U(3.W))

    def answer(op: Operation.Type): Response.Type = {
      Mux(op === Operation.Get, AccessAckData, AccessAck)
    }
  }

  // (address & (2^size -1) == 0) always holds

  case class Parameters(
                              w: Int, // Width of the data bus in bytes
                              a: Int, // Width of each address field in bits
                              z: Int, // Width of each size field in bits
                              o: Option[Int] = None, // Number of bits to disambiguate per-link master sources
                              i: Option[Int] = None // Number of bits to disambiguate per-link slaves
                              )

  abstract class Channel extends Bundle {
    val params: Tilelink.Parameters
  }
  /*
  val valid = Output(Bool()) // The sender is offering progress on an operation.
    val ready = Input(Bool()) // The receiver accepted the offered progress.
   */
  object Channel {
    object A {
      def apply(params: Tilelink.Parameters): A = new A(params)
    }
    class A(val params: Tilelink.Parameters) extends Channel {
      import params._
      val opcode = Tilelink.Operation() // Operation code. Identifies the type of message carried by the channel.
      val param = UInt(3.W) // Parameter code. Meaning depends on a_opcode; specifies a transfer of caching permissions or a sub-opcode.
      val size = UInt(z.W) // Logarithm of the operation size: 2^z bytes
      val source = if(o.isDefined) UInt(o.get.W) else UInt() // Per-link master source identifier.
      val address = UInt(a.W) // Target byte address of the operation. Must be aligned to size.
      val mask = Vec(w, Bool()) // Byte lane select for messages with data.
      val data = Vec(w, Byte()) // Data payload for messages with data.
      val corrupt = Bool() // The data in this beat is corrupt.
    }
    object D {
      def apply(params: Tilelink.Parameters): D = new D(params)
    }
    class D(val params: Tilelink.Parameters) extends Channel {
      import params._
      val opcode = Tilelink.Response() // Operation code. Identifies the type of message carried by the channel.
      val param = UInt(2.W) // Parameter code. Meaning depends on d_opcode; specifies permissions to transfer or a sub-opcode.
      val size = UInt(z.W) // Logarithm of the operation size: 2^z bytes.
      val source = if(o.isDefined) UInt(o.get.W) else UInt() // Per-link master source identifier. (S
      val sink = if(i.isDefined) UInt(i.get.W) else UInt() // Per-link slave sink identifier.
      val denied = Bool() // The slave was unable to service the request.
      val data = Vec(w,Byte()) // Data payload for messages with data.
      val corrupt = Bool() // Corruption was detected in the data payload.
    }

  }


  object Agent {
    abstract class Interface extends Bundle {

    }
    object Interface {
      object Requester {
        def apply(params: Tilelink.Parameters): Requester = new Requester(params)
      }
      class Requester(val params: Tilelink.Parameters) extends Interface {
        val a = Decoupled(Channel.A(params))
        val d = Decoupled(Channel.D(params)).flipped

        def <=>(responder: Responder): Unit = {
          this.a <> responder.a
          this.d <> responder.d
        }
      }
      object Responder {
        def apply(params: Tilelink.Parameters): Responder = new Responder(params)
      }
      class Responder(val params: Tilelink.Parameters) extends Interface {
        val a = Decoupled(Channel.A(params)).flipped
        val d = Decoupled(Channel.D(params))

        def <=>(requester: Requester): Unit = {
          this.a <> requester.a
          this.d <> requester.d
        }
      }
    }
  }



  /*
  abstract class Agent() extends Module {

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
*/


}
