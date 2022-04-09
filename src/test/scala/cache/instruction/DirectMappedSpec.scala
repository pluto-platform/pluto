package cache.instruction

import cache.Cache
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class DirectMappedSpec extends AnyFlatSpec with ChiselScalatestTester {

  it should "run" in {
    test(new DirectMapped(Cache.Dimension(1024, 32, BigInt(0) until BigInt(0x10000)))).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.io.fillPort.data.poke(0xFF.U)
      dut.io.fillPort.valid.poke(1.B)

      dut.io.request.valid.poke(1.B)
      dut.io.request.address.poke(4.U)

      dut.clock.step()



      dut.io.request.valid.poke(1.B)
      dut.io.request.address.poke(32.U)

      dut.clock.step(50)

    }
  }

}
