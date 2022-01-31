package core.pipeline

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chiseltest._
import core.pipeline.IntegerRegisterFile.WritePort
import org.scalatest.flatspec.AnyFlatSpec
import lib.ValidTesting._
import lib.RandomHelper._

class IntegerRegisterFileSpec extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "IntegerRegisterFile"

  it should "internally forward a value to source" in {
    test(new IntegerRegisterFile) { dut =>
      val (address,data) = (uRand(1 until 32),uRand(32.W))

      dut.io.write.send((new WritePort).Lit(
        _.address -> address,
        _.data -> data
      ))
      dut.io.source.foreach(s => s.address.poke(address))

      dut.io.source.foreach(s => s.data.expect(data))
    }
  }

  it should "write two values and read them independently" in {
    test(new IntegerRegisterFile) { dut =>
      val (address,data) = (uRands(1 until 32, 1 until 32), uRands(32.W, 32.W))

      address.zip(data).foreach { case (a,d) =>
        dut.io.write.send((new WritePort).Lit(
          _.address -> a,
          _.data -> d
        ))

        dut.clock.step()
      }

      dut.io.write.choke()

      dut.io.source.zip(address).foreach { case (s,a) => s.address.poke(a) }

      dut.clock.step()

      dut.io.source.zip(data).foreach { case (s,d) => s.data.expect(d) }
    }
  }

  it should "keep x0 to be 0" in {
    test(new IntegerRegisterFile) { dut =>
      val data = uRand(32.W)

      dut.io.write.send((new WritePort).Lit(
        _.address -> 0.U,
        _.data -> data
      ))
      dut.io.source.foreach(s => s.address.poke(0.U))
      dut.io.source.foreach(s => s.data.expect(0.U))

      dut.clock.step()
      dut.io.write.bits.address.poke(uRand(1 until 32))

      dut.io.source.foreach(s => s.data.expect(0.U))

    }
  }

  it should "not forward values for x0" in {
    test(new IntegerRegisterFile) { dut =>
      val data = uRand(1 until 1024)

      dut.io.write.send((new WritePort).Lit(
        _.address -> 0.U,
        _.data -> data
      ))
      dut.io.source.foreach(s => s.address.poke(0.U))

      dut.io.source.foreach(s => s.data.expect(0.U))

    }
  }

  it should "accept writes for all 32 registers" in {
    test(new IntegerRegisterFile) { dut =>
      val data = Seq.fill(32)(uRand(32.W))

      (0 until 32).zip(data).foreach { case (a,d) =>
        dut.io.write.send((new WritePort).Lit(
          _.address -> a.U,
          _.data -> d
        ))
        dut.clock.step()
      }

      dut.io.write.bits.address.poke(0.U)
      dut.io.write.choke()

      (0 until 32).zip(data).foreach { case (a,d) =>
        dut.io.source.foreach { s =>
          s.address.poke(a.U)
          dut.clock.step()
          s.data.expect(if(a == 0) 0.U else d)
        }
      }

    }
  }


}
