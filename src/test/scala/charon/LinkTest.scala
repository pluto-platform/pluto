package charon

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class LinkTest extends AnyFlatSpec with ChiselScalatestTester {

  "seq" should "work" in {
    test(new CharonTest).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.step(100)
    }
  }

}



object LinkExample extends App {
  emitVerilog(
    new Linker(3, Seq(
      Seq(AddressRange(0x00, 1024)),
      Seq(AddressRange(0x100, 4096)),
      Seq(AddressRange(0x400,0x1000000))
    ), Tilelink.Parameters(4, 32, 2, Some(5), Some(5)))
  )
}