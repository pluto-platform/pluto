package core.pipeline

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chiseltest._
import core.pipeline.IntegerRegisterFile.{WriteRequest}
import org.scalatest.flatspec.AnyFlatSpec
import lib.ValidTesting._
import lib.RandomHelper._

class IntegerRegisterFileSpec extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "IntegerRegisterFile"

  it should "internally forward a value to source" in {
    test(new IntegerRegisterFile) { dut =>
      val (address,data) = (uRand(1 until 32),uRand(32.W))

      dut.io.write.send((new WriteRequest).Lit(
        _.index -> address,
        _.data -> data,
      ))
      dut.io.source.request.index.foreach(s => s.poke(address))

      dut.io.source.response.data.foreach(s => s.expect(data))
    }
  }

  it should "write two values and read them independently" in {
    test(new IntegerRegisterFile) { dut =>
      val (address,data) = (uRands(1 until 32, 1 until 32), uRands(32.W, 32.W))

      address.zip(data).foreach { case (a,d) =>
        dut.io.write.send((new WriteRequest).Lit(
          _.index -> a,
          _.data -> d
        ))

        dut.clock.step()
      }

      dut.io.write.choke()

      dut.io.source.request.index.zip(address).foreach { case (s,a) => s.poke(a) }

      dut.clock.step()

      dut.io.source.response.data.zip(data).foreach { case (s,d) => s.expect(d) }
    }
  }

  it should "keep x0 to be 0" in {
    test(new IntegerRegisterFile) { dut =>
      val data = uRand(32.W)

      dut.io.write.send((new WriteRequest).Lit(
        _.index -> 0.U,
        _.data -> data
      ))
      dut.io.source.request.index.foreach(s => s.poke(0.U))
      dut.io.source.response.data.foreach(s => s.expect(0.U))

      dut.io.write.bits.index.poke(uRand(1 until 32))

      dut.io.source.response.data.foreach(s => s.expect(0.U))

    }
  }

  it should "not forward values for x0" in {
    test(new IntegerRegisterFile) { dut =>
      val data = uRand(1 until 1024)

      dut.io.write.send((new WriteRequest).Lit(
        _.index -> 0.U,
        _.data -> data
      ))
      dut.io.source.request.index.foreach(s => s.poke(0.U))

      dut.io.source.response.data.foreach(s => s.expect(0.U))

    }
  }

  it should "accept writes for all 32 registers" in {
    test(new IntegerRegisterFile) { dut =>
      val data = Seq.fill(32)(uRand(32.W))

      (0 until 32).zip(data).foreach { case (a,d) =>
        dut.io.write.send((new WriteRequest).Lit(
          _.index -> a.U,
          _.data -> d
        ))
        dut.clock.step()
      }

      dut.io.write.bits.index.poke(0.U)
      dut.io.write.choke()

      (0 until 32).zip(data).foreach { case (a,d) =>
        dut.io.source.request.index.zip(dut.io.source.response.data).foreach { case (index,data) =>
          index.poke(a.U)
          dut.clock.step()
          data.expect(if(a == 0) 0.U else d)
        }
      }

    }
  }


}
