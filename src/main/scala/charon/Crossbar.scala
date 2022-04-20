package charon

import chisel3._






object Crossbar {

  def apply(requesters: Tilelink.Agent.Interface.Requester*)(responders: Tilelink.Agent.Interface.Responder*)(addressMap: AddressMap)(implicit params: Tilelink.Parameters): Crossbar = {

    val c = Module(new Crossbar(requesters.length, responders.length)(addressMap))
    c.io.requesters.zip(requesters).foreach { case (port,signal) => port := signal }
    c.io.responders.zip(responders).foreach { case (port,signal) => port := signal }

    c
  }
}

class Crossbar(n: Int, m: Int)(addressMap: AddressMap)(implicit params: Tilelink.Parameters) extends Module {

  val io = IO(new Bundle {
    val requesters = Vec(n, Flipped(new Tilelink.Agent.Interface.Requester(params)))
    val responders = Vec(m, Flipped(new Tilelink.Agent.Interface.Responder(params)))
  })

  io.responders.foreach { responder =>

    responder.a <> ChannelMux(io.requesters.map(_.a), addressMap.map(responder))

  }

  io.requesters.zipWithIndex.foreach { case (requester, i) =>

    requester.d <> ChannelMux(io.responders.map(_.d), i.U)

  }

}