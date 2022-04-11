package cache.instruction

import cache.Cache
import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chiseltest._
import lib.RandomHelper.uRand
import org.scalatest.flatspec.AnyFlatSpec

class DirectMappedSpec extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Direct-mapped instruction cache"

  it should "handle sequential reads" in {
    val size = 0x1000
    cacheTest(
      Cache.Dimension(1024, 32, BigInt(0) until BigInt(size)),
      Seq.fill(size)(uRand(32.W)),
      Seq.tabulate(size/4)(i => i*4)
    )
  }
  it should "handle random reads" in {
    val size = 0x1000
    cacheTest(
      Cache.Dimension(1024, 32, BigInt(0) until BigInt(size)),
      Seq.fill(size)(uRand(32.W)),
      Seq.fill(300)(uRand(0 until (size/4)).litValue.toInt)
    )
  }

  def cacheTest(dim: Cache.Dimension, mem: Seq[UInt], accesses: Seq[Int]) = {
    test(new DirectMapped(dim)) { dut =>

      val expectedStream = accesses.map(i => mem(i/4))

      dut.io.request.initSource().setSourceClock(dut.clock)
      dut.io.response.initSink().setSinkClock(dut.clock)

      fork {
        while (true) {
          val req = dut.io.fillPort.fill.peek.litToBoolean
          val addr = dut.io.fillPort.address.peekInt.toInt / 4
          val len = dut.io.fillPort.length.peekInt.toInt
          dut.clock.step()
          if (req) {
            for (i <- 0 until len) {
              dut.io.fillPort.valid.poke(1.B)
              dut.io.fillPort.data.poke(mem(addr + i))
              dut.clock.step()
            }
            dut.io.fillPort.valid.poke(0.B)
          }
        }
      }

      fork{
        dut.io.request.enqueueSeq(accesses.map(a => new InstructionCache.Request()(dut.dim).Lit(_.address -> a.U)))
      }.fork {
        dut.io.response.expectDequeueSeq(expectedStream.map(v => new InstructionCache.Response().Lit(_.instruction -> v)))
      }.join()

    }
  }

}
