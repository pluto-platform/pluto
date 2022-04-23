package charon

import chisel3._
import chisel3.util.{Decoupled, DecoupledIO, Queue, log2Ceil}
import lib.util.{BoolVec, InputOutputExtender, SeqToVecMethods, pow2}

object Charon {

  implicit class RangeBinder(responder: Tilelink.Agent.Interface.Responder) {
    def bind(address: Int): (Tilelink.Agent.Interface.Responder, Seq[AddressRange]) = (responder, Seq(AddressRange(address, pow2(responder.params.a))))
  }

  object Link {
    def apply(requesters: Seq[Tilelink.Agent.Interface.Requester], responders: Seq[(Tilelink.Agent.Interface.Responder, Seq[AddressRange])]): Unit = {

    }
    def apply(requester: Tilelink.Agent.Interface.Requester, responders: Seq[(Tilelink.Agent.Interface.Responder, Seq[AddressRange])]): Unit = apply(Seq(requester), responders)
    def apply(requesters: Seq[Tilelink.Agent.Interface.Requester], responder: (Tilelink.Agent.Interface.Responder, Seq[AddressRange])): Unit = apply(requesters, Seq(responder))
    def apply(requester: Tilelink.Agent.Interface.Requester, responder: (Tilelink.Agent.Interface.Responder, Seq[AddressRange])): Unit = apply(Seq(requester), Seq(responder))
  }

  object Bridge {
    def apply(params: Tilelink.Parameters): Bridge = Module(new Bridge(params))
  }
  class Bridge(params: Tilelink.Parameters) extends Module {
    val io = IO(new Bundle {
      val requester = Tilelink.Agent.Interface.Requester(params)
      val responder = Tilelink.Agent.Interface.Responder(params)
    })
    io.requester <=> io.responder
  }
  class Dbridge(params: Tilelink.Parameters) extends Module {
    val io = IO(new Bundle {
      val in = Tilelink.Channel.D(params).flipped
      val out = Tilelink.Channel.D(params)
    })
    io.in <> io.out
  }

  object Combine {
    def apply(responders: Seq[(Tilelink.Agent.Interface.Responder, Seq[AddressRange])]): Tilelink.Agent.Interface.Responder = {

      val addressWidth = log2Ceil(responders.flatMap(_._2.map(r => r.base + r.length)).max)

      val params = Tilelink.Parameters(4, addressWidth, 2, Some(10), Some(log2Ceil(responders.length)))

      val combiner = Module(new Combiner(responders.map(_._2), params))

      combiner.io.leafs
        .zip(responders.map(_._1))
        .foreach { case (port, responder) =>
          port <> responder
        }

      combiner.io.root

    }
  }

}

object ChannelBuffer {
  def apply[T <: Tilelink.Channel](channel: DecoupledIO[T], depth: Int = 1): DecoupledIO[T] = {
    val buf = Module(new ChannelBuffer(chiselTypeOf(channel.bits), depth))
    buf.io.enqueue <> channel
    buf.io.dequeue
  }
}
class ChannelBuffer[T <: Tilelink.Channel](channel: => T, depth: Int) extends Module {
  val io = IO(new Bundle {
    val enqueue = Decoupled(channel).flipped
    val dequeue = Decoupled(channel)
  })

  io.dequeue <> Queue(io.enqueue, depth)

}

class Combiner(addresses: Seq[Seq[AddressRange]], params: Tilelink.Parameters) extends Module {

  val io = IO(new Bundle {
    val root = Tilelink.Agent.Interface.Responder(params)
    val leafs = Vec(addresses.length, Tilelink.Agent.Interface.Requester(params))
  })

  io.root.a.ready := 0.B
  io.leafs
    .zip(addresses)
    .foreach { case (leaf, addressRanges) =>
      leaf.a.bits := io.root.a.bits
      val addressHit = addressRanges.map(_.contains(io.root.a.bits.address)).orR
      when(addressHit) {
        leaf.a.valid := io.root.a.valid
        io.root.a.ready := leaf.a.ready
      } otherwise {
        leaf.a.valid := 0.B
      }
    }

  io.root.d <> io.leafs
    .map(_.d)
    .reduce { (l,r) =>
      ChannelMux(l,r)
    }


}

object ChannelMux {
  def apply[T <: Tilelink.Channel](left: DecoupledIO[T], right: DecoupledIO[T]): DecoupledIO[T] = {
    val mux = Module(new ChannelMux(chiselTypeOf(left.bits), left.bits.params))
    mux.io.in <> VecInit(left, right)
    mux.io.out
  }
}
class ChannelMux[T <: Tilelink.Channel](channel: => T, params: Tilelink.Parameters) extends Module {
  val io = IO(new Bundle {
    val in = Vec(2, Decoupled(channel)).flipped
    val out = Decoupled(channel)
  })
  io.in.foreach(_.ready := 0.B)
  when(io.in(0).valid) {
    io.out <> io.in(0)
  } otherwise {
    io.out <> io.in(1)
  }

}
