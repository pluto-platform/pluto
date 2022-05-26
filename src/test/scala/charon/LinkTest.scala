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

  "mux" should "work" in {
    test(new ChannelMux(Tilelink.Channel.A(Tilelink.Parameters(4,16,2,Some(2),Some(2))))).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.io.in(0).valid.poke(1.B)
      dut.io.in(0).bits.source.poke(0.U)
      dut.io.in(0).bits.address.poke(0xDEAD.U)

      dut.io.in(1).valid.poke(1.B)
      dut.io.in(1).bits.source.poke(1.U)
      dut.io.in(1).bits.address.poke(0xBEEF.U)

      dut.io.out.valid.expect(1.B)
      dut.io.out.bits.source.expect(0.U)
      dut.io.out.bits.address.expect(0xDEAD.U)

      dut.clock.step(2)

      dut.io.out.valid.expect(1.B)
      dut.io.out.bits.source.expect(0.U)
      dut.io.out.bits.address.expect(0xDEAD.U)

      dut.io.out.ready.poke(1.B)
      dut.clock.step()
      dut.io.out.ready.poke(0.B)

      dut.io.out.valid.expect(1.B)
      dut.io.out.bits.source.expect(1.U)
      dut.io.out.bits.address.expect(0xBEEF.U)

      dut.io.in(0).valid.poke(0.B)

      dut.io.out.ready.poke(1.B)
      dut.clock.step()
      dut.io.out.ready.poke(0.B)

      dut.io.in(1).bits.address.poke(0xAAAA.U)

      dut.io.out.valid.expect(1.B)
      dut.io.out.bits.source.expect(1.U)
      dut.io.out.bits.address.expect(0xAAAA.U)

      dut.io.out.ready.poke(1.B)
      dut.clock.step()
      dut.io.out.ready.poke(0.B)

      dut.io.in(1).valid.poke(0.B)

      dut.io.out.valid.expect(0.B)


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