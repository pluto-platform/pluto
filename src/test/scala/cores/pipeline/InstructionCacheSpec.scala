package cores.pipeline
/*
import cache.InstructionCache
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class InstructionCacheSpec extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "InstructionCache"

  it should "be nice" in {
    test(new InstructionCache(CacheDimension(1024,4))).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.io.request.valid.poke(1.B)
      dut.io.request.bits.address.poke(24.U)

      dut.io.memory.transfering.poke(1.B)
      dut.io.memory.readData.poke(0xDEADBEEFL.U)

      dut.clock.step(8)

      dut.io.request.bits.address.poke(28.U)

      dut.clock.step(4)

      dut.io.request.bits.address.poke(32.U)

      dut.clock.step(8)


    }
  }

}


 */