package cache.instruction

import chisel3._
import cache.Cache
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class FillFsmSpec extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "fill FSM"

  it should "work" in {
    test(new FillFsm(Cache.Dimension(2048, 32, BigInt(0) until BigInt(0x1000)))).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.io.fillreq.fill.poke(1.B)
      dut.io.fillreq.address.poke(0x100.U)
      dut.io.fillreq.length.poke(8.U)

      dut.clock.step(20)

    }
  }

}
