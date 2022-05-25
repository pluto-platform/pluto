package charon

import chisel3._
import chisel3.util.{Decoupled, DecoupledIO, Queue, log2Ceil}
import lib.util.{BoolVec, BundleItemAssignment, InputOutputExtender, SeqToVecMethods, pow2}

object Charon {

  implicit class RangeBinder(responder: Tilelink.Agent.Interface.Responder) {
    def bind(address: Int): (Tilelink.Agent.Interface.Responder, Seq[AddressRange]) = (responder, Seq(AddressRange(address, pow2(responder.params.a))))
    def bind(address: Long): (Tilelink.Agent.Interface.Responder, Seq[AddressRange]) = (responder, Seq(AddressRange(address, pow2(responder.params.a))))
  }

  object Link {
    def apply(requesters: Seq[Tilelink.Agent.Interface.Requester], responders: Seq[(Tilelink.Agent.Interface.Responder, Seq[AddressRange])]): Unit = {
      val linker = Module(new Linker(requesters.length, responders.map(_._2), requesters.head.params))
      linker.io.in.zip(requesters).foreach { case (port, signal) => port <> signal }
      linker.io.out.zip(responders.map(_._1)).foreach { case (port, signal) => port <> signal }
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
object SourceSetter {
  def apply(channel: DecoupledIO[Tilelink.Channel.A], id: Int): DecoupledIO[Tilelink.Channel.A] = {
    val setter = Module(new SourceSetter(id, channel.bits.params))
    setter.io.in <> channel
    setter.io.out
  }
}
class SourceSetter(id: Int, params: Tilelink.Parameters) extends Module {
  val io = IO(new Bundle {
    val in = Decoupled(Tilelink.Channel.A(params)).flipped
    val out = Decoupled(Tilelink.Channel.A(params))
  })

  io.out <> io.in
  io.out.bits.source := id.U

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

  io.root.d <> ChannelMux(io.leafs.map(_.d))


}

class Linker(n: Int, addresses: Seq[Seq[AddressRange]], params: Tilelink.Parameters) extends Module {
  val io = IO(new Bundle {
    val in = Vec(n, Tilelink.Agent.Interface.Responder(params))
    val out = Vec(addresses.length, Tilelink.Agent.Interface.Requester(params))
  })

  val errorSlaves = Seq.fill(n)(Module(new ErrorSlave(params)))

  val as = io.in
    .map(_.a)
    .zipWithIndex
    .map { case (channel, index) =>
      SourceSetter(channel, index)
    }
    .map(ChannelBuffer(_))

  val requestMatrix = as
    .zip(errorSlaves.map(_.io.tilelink.a))
    .map { case (a, err) => ChannelDemuxA(a, addresses, err) }
    .transpose
    .map(_.map(ChannelBuffer(_)))

  io.out
    .map(_.a)
    .zip(requestMatrix)
    .foreach { case (c,req) =>
      c <> ChannelBuffer(ChannelMux(req))
    }

  val ds = io.out
    .map(_.d)
    .map(ChannelBuffer(_))

  val responseMatrix = ds
    .map(ChannelDemuxD(_, io.in.length))
    .transpose
    .map(_.map(ChannelBuffer(_)))

  io.in
    .map(_.d)
    .zip(responseMatrix)
    .zip(errorSlaves.map(_.io.tilelink.d))
    .foreach { case ((c,res),err) =>
      c <> ChannelBuffer(ChannelMux(res :+ err))
    }

}

object ChannelMux {
  def apply[T <: Tilelink.Channel](left: DecoupledIO[T], right: DecoupledIO[T]): DecoupledIO[T] = {
    val mux = Module(new ChannelMux(chiselTypeOf(left.bits)))
    mux.io.in(0) <> left
    mux.io.in(1) <> right
    mux.io.out
  }
  def apply[T <: Tilelink.Channel](channels: Seq[DecoupledIO[T]]): DecoupledIO[T] = {
    channels.toVec.reduceTree { (l,r) => ChannelMux(l,r) }
  }
}
class ChannelMux[T <: Tilelink.Channel](channel: => T) extends Module {
  val io = IO(new Bundle {
    val in = Vec(2, Decoupled(channel)).flipped
    val out = Decoupled(channel)
  })

  val prevTurnReg = RegInit(0.B)

  val contention = io.in(0).valid && io.in(1).valid

  val aTurn = io.in(0).valid || (contention && prevTurnReg)
  val bTurn = io.in(1).valid || (contention && !prevTurnReg)

  io.in.foreach(_.ready := 0.B)
  when(aTurn) {
    io.out <> io.in(0)
  } otherwise {
    io.out <> io.in(1)
  }
  when(aTurn) {
    prevTurnReg := 0.B
  }.elsewhen(bTurn) {
    prevTurnReg := 1.B
  }

}


object ChannelDemuxA {
  def apply(channel: DecoupledIO[Tilelink.Channel.A], ranges: Seq[Seq[AddressRange]], errorSlave: DecoupledIO[Tilelink.Channel.A]): Seq[DecoupledIO[Tilelink.Channel.A]] = {
    val demux = Module(new ChannelDemuxA(ranges, channel.bits.params))
    demux.io.in <> channel
    demux.io.errorSlave <> errorSlave
    demux.io.out
  }
}
class ChannelDemuxA(ranges: Seq[Seq[AddressRange]], params: Tilelink.Parameters) extends Module {
  val io = IO(new Bundle {
    val in = Decoupled(Tilelink.Channel.A(params)).flipped
    val out = Vec(ranges.length, Decoupled(Tilelink.Channel.A(params)))
    val errorSlave = Decoupled(Tilelink.Channel.A(params))
  })

  io.in.ready := 0.B

  val hits = io.out
    .zip(ranges)
    .map { case (leaf, range) =>
      range.map(_.contains(io.in.bits.address)).orR
    }.toVec

  io.errorSlave.bits <> io.in.bits
  when(!hits.orR) {
    io.errorSlave.valid := 1.B
    io.in.ready := 1.B
  } otherwise {
    io.errorSlave.valid := 0.B
  }

  io.out
    .zip(hits)
    .foreach { case (leaf, hit) =>
      leaf.bits <> io.in.bits
      when(hit) {
        leaf.valid := io.in.valid
        io.in.ready := leaf.ready
      } otherwise {
        leaf.valid := 0.B
      }
    }
}

class ErrorSlave(params: Tilelink.Parameters) extends Module {
  val io = IO(new Bundle {
    val tilelink = Tilelink.Agent.Interface.Responder(params)
  })
  io.tilelink.a.ready := 1.B
  io.tilelink.d.set(
    _.valid := RegNext(io.tilelink.a.valid, 0.B),
    _.bits.set(
      _.opcode := Mux(io.tilelink.a.bits.opcode === Tilelink.Operation.Get, Tilelink.Response.AccessAckData, Tilelink.Response.AccessAck),
      _.param := 0.U,
      _.size := RegNext(io.tilelink.a.bits.size, 0.U),
      _.source := RegNext(io.tilelink.a.bits.source, 0.U),
      _.sink := ((1 << params.i.get) - 1).U,
      _.denied := 1.B,
      _.data := Seq.fill(4)(0.U).toVec,
      _.corrupt := 0.B
    )
  )
}

object ChannelDemuxD {
  def apply(channel: DecoupledIO[Tilelink.Channel.D], n: Int): Seq[DecoupledIO[Tilelink.Channel.D]] = {
    val demux = Module(new ChannelDemuxD(n, channel.bits.params))
    demux.io.in <> channel
    demux.io.out
  }
}
class ChannelDemuxD(n: Int, params: Tilelink.Parameters) extends Module {
  val io = IO(new Bundle {
    val in = Decoupled(Tilelink.Channel.D(params)).flipped
    val out = Vec(n, Decoupled(Tilelink.Channel.D(params)))
  })

  io.in.ready := 0.B

  io.out
    .zipWithIndex
    .foreach { case (leaf, index) =>
      leaf.bits <> io.in.bits
      when(io.in.bits.source === index.U) {
        leaf.valid := io.in.valid
        io.in.ready := leaf.ready
      } otherwise {
        leaf.valid := 0.B
      }
    }
}