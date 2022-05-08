package charon

import chisel3._


case class AddressRange(base: BigInt, length: BigInt) {

  def contains(that: UInt): Bool = that >= base.U && that < (base+length).U

}
