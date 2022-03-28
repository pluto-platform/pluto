package charon

import chisel3._
import lib.util.SeqToVecMethods

object Arbiter {

  def apply[T <: Tilelink.Channel](left: T, right: T): T = {
    val arbiter = Module(new Arbiter(chiselTypeOf(left)))
    arbiter.in(0) := left
    arbiter.in(1) := right
    arbiter.out
  }

  def apply[T <: Tilelink.Channel](channels: Vec[T]): T = {
    channels.reduceTree((l,r) => apply(l,r))
  }

}

class Arbiter[T <: Tilelink.Channel](gen: => T) extends Module {

  val in = IO(Vec(2, Flipped(gen)))
  val out = IO(gen)

  when(in(0).valid) {
    out <> in(0)
  } otherwise {
    out <> in(1)
  }

}

object ChannelMux {
  def apply(channels: Seq[Tilelink.Channel.A], addressRange: AddressRange): Tilelink.Channel.A = {
    Arbiter(channels.map(_.filterByRange(addressRange)).toVec)
  }
  def apply(channels: Seq[Tilelink.Channel.D], source: UInt): Tilelink.Channel.D = {
    Arbiter(channels.map(_.filterBySource(source)).toVec)
  }
}
