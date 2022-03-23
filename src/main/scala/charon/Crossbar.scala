package charon

import Chisel.DecoupledIO
import charon.Tilelink.Interface
import chisel3._
import chisel3.util.{Arbiter, MuxCase, MuxLookup}
import lib.util.SeqToVecMethods

object Crossbar {

  def apply(managers: Tilelink.Interface.Manager*)(clients: Tilelink.Interface.Client*)(addressMap: AddressMap)(implicit params: Tilelink.LinkParameters): Crossbar = {

    val sinkLookUp = (m: Tilelink.Interface.Manager) => VecInit(clients.map(addressMap.map(_)).map { _.contains(m.a.address) })

    val c = Module(new Crossbar(managers.length, clients.length)(sinkLookUp))
    c.io.managers.zip(managers).foreach { case (port,signal) => port := signal }
    c.io.clients.zip(clients).foreach { case (port,signal) => port := signal }

    c
  }
}

class Crossbar(n: Int, m: Int)(slaveSelect: Tilelink.Interface.Manager => Vec[Bool])(implicit params: Tilelink.LinkParameters) extends Module {

  val io = IO(new Bundle {
    val managers = Vec(n, Flipped(new Tilelink.Interface.Manager))
    val clients = Vec(m, Flipped(new Tilelink.Interface.Client))
  })


  io.clients.map(_.a).zip(io.managers.map(slaveSelect).transpose).foreach { case (slave, masterSelects) =>

    val winner = Arbiter(io.managers.map(_.a), masterSelects)

    slave <> winner

  }



}

object Arbiter {

  def apply[T <: Tilelink.Channel](channels: Seq[T], req: Seq[Bool]): T = {
    val cs = channels.map(_.cloneType)
    (cs, channels, req).zipped.map { case (w, c, req) =>
      w <> c
      w.valid := c.valid && req
    }
    cs.toVec.reduceTree { (l, r) =>
      val wire = Wire(l.cloneType)
      when(l.valid) {
        wire <> l
      } otherwise {
        wire <> r
      }
      l.ready := wire.ready
      r.ready := wire.ready && !l.valid
      wire
  }

}
class Arbiter(n: Int) extends Module {

  val

}