package charon

import chisel3._

object AddressMap {

}
case class AddressMap(map: Map[Tilelink.Interface.Responder,AddressRange]) {

}



case class AddressRange(base: BigInt, length: BigInt) {

  def contains(that: UInt): Bool = that > base.U && that < (base+length).U

}
