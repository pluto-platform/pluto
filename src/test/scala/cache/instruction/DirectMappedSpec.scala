package cache.instruction

import cache.Cache
import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chiseltest._
import lib.RandomHelper.uRand
import org.scalatest.flatspec.AnyFlatSpec

class DirectMappedSpec extends AnyFlatSpec with ChiselScalatestTester {

  it should "run" in {

    val size = 0x10000

    test(new DirectMapped(Cache.Dimension(1024, 32, BigInt(0) until BigInt(size)))).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      val mem = Seq.tabulate(size)(i => (i*4).U)

      val accesses = Seq.fill(100)(uRand(0 until (size/4)).litValue.toInt * 4)

      val expectedStream = accesses.map(mem(_))

      dut.io.request.initSource().setSourceClock(dut.clock)
      dut.io.response.initSink().setSinkClock(dut.clock)

      fork {
        while (true) {
          val req = dut.io.fillPort.fill.peek.litToBoolean
          val addr = dut.io.fillPort.address.peekInt.toInt / 4
          val len = dut.io.fillPort.length.peekInt.toInt
          println(s"$req -> $addr ($len)")
          dut.clock.step()
          if (req) {
            println("request!")
            for (i <- 0 until len) {
              dut.io.fillPort.valid.poke(1.B)
              dut.io.fillPort.data.poke(mem(addr + i))
              println(s"sending ${mem(addr + i)}")
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
