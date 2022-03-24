package charon

import chisel3._


object Crossbar {

  def apply(managers: Tilelink.Interface.Manager*)(clients: Tilelink.Interface.Client*)(addressMap: AddressMap)(implicit params: Tilelink.LinkParameters): Crossbar = {

    val c = Module(new Crossbar(managers.length, clients.length)(addressMap))
    c.io.managers.zip(managers).foreach { case (port,signal) => port := signal }
    c.io.clients.zip(clients).foreach { case (port,signal) => port := signal }

    c
  }
}

class Crossbar(n: Int, m: Int)(addressMap: AddressMap)(implicit params: Tilelink.LinkParameters) extends Module {

  val io = IO(new Bundle {
    val managers = Vec(n, Flipped(new Tilelink.Interface.Manager))
    val clients = Vec(m, Flipped(new Tilelink.Interface.Client))
  })


  io.clients.foreach { client =>

    client.a <> ChannelMux(io.managers.map(_.a), addressMap.map(client))

  }

  io.managers.zipWithIndex.foreach { case (manager, i) =>

    manager.d <> ChannelMux(io.clients.map(_.d), i.U)

  }

}