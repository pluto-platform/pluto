package charon

object Charon {

  implicit class RangeBinder(responder: Tilelink.Agent.Interface.Responder) {
    def bind(address: Int): (Tilelink.Agent.Interface.Responder, Seq[AddressRange]) = (responder, Seq(AddressRange(address, responder.size)))
  }

  object Link {
    def apply(requesters: Seq[Tilelink.Agent.Interface.Requester], responders: Seq[(Tilelink.Agent.Interface.Responder, Seq[AddressRange])]): Unit = {

    }
    def apply(requester: Tilelink.Agent.Interface.Requester, responders: Seq[(Tilelink.Agent.Interface.Responder, Seq[AddressRange])]): Unit = apply(Seq(requester), responders)
    def apply(requesters: Seq[Tilelink.Agent.Interface.Requester], responder: (Tilelink.Agent.Interface.Responder, Seq[AddressRange])): Unit = apply(requesters, Seq(responder))
    def apply(requester: Tilelink.Agent.Interface.Requester, responder: (Tilelink.Agent.Interface.Responder, Seq[AddressRange])): Unit = apply(Seq(requester), Seq(responder))
  }

  object Combine {
    def apply(responders: Seq[Tilelink.Agent.Interface.Responder]): (Tilelink.Agent.Interface.Responder, Seq[AddressRange]) = ???
  }

}

