package plutocore.pipeline

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chiseltest._
import plutocore.pipeline.IntegerRegisterFile.WriteRequest
import org.scalatest.flatspec.AnyFlatSpec
import lib.ValidTesting._
import lib.RandomHelper._

class IntegerRegisterFileSpec extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "IntegerRegisterFile"

  import IntegerRegisterFileSpec.IntegerRegisterFileWrapper

  it should "internally forward a value to source" in {
    test(new IntegerRegisterFile) { dut =>
      val (index,data) = (uRand(1 until 32),uRand(32.W))

      dut.applyWriteRequest(index,data)
      dut.applyReadRequest.foreach(s => s.poke(index))

      dut.getReadResponse.foreach(s => s.expect(data))
    }
  }

  it should "write two values and read them independently" in {
    test(new IntegerRegisterFile) { dut =>
      val (address,data) = (uRands(1 until 32, 1 until 32), uRands(32.W, 32.W))

      address.zip(data).foreach { case (a,d) =>
        dut.applyWriteRequest(a,d)
        dut.clock.step()
      }

      dut.unapplyWriteRequest()

      dut.applyReadRequest.zip(address).foreach { case (s,a) => s.poke(a) }

      dut.clock.step()

      dut.getReadResponse.zip(data).foreach { case (s,d) => s.expect(d) }
    }
  }

  it should "keep x0 to be 0" in {
    test(new IntegerRegisterFile) { dut =>
      val data = uRand(32.W)

      dut.applyWriteRequest(0.U,data)
      dut.applyReadRequest.foreach(s => s.poke(0.U))
      dut.getReadResponse.foreach(s => s.expect(0.U))

      dut.applyReadRequest.foreach(_.poke(uRand(1 until 32)))

      dut.getReadResponse.foreach(s => s.expect(0.U))

    }
  }

  it should "not forward values for x0" in {
    test(new IntegerRegisterFile) { dut =>
      val data = uRand(1 until 1024)

      dut.applyWriteRequest(0.U,data)
      dut.applyReadRequest.foreach(_.poke(0.U))

      dut.getReadResponse.foreach(_.expect(0.U))

    }
  }

  it should "accept writes for all 32 registers" in {
    test(new IntegerRegisterFile) { dut =>
      val data = Seq.fill(32)(uRand(32.W))

      (0 until 32).zip(data).foreach { case (a,d) =>
        dut.applyWriteRequest(a.U,d)
        dut.clock.step()
      }

      dut.io.write.bits.index.poke(0.U)
      dut.io.write.choke()

      (0 until 32).zip(data).foreach { case (a,d) =>
        dut.applyReadRequest.zip(dut.getReadResponse).foreach { case (index,data) =>
          index.poke(a.U)
          dut.clock.step()
          data.expect(if(a == 0) 0.U else d)
        }
      }

    }
  }


}


object IntegerRegisterFileSpec {
  implicit class IntegerRegisterFileWrapper(dut: IntegerRegisterFile) {
    def applyWriteRequest(idx: UInt, data: UInt): Unit = {
      dut.io.write.valid.poke(1.B)
      dut.io.write.bits.index.poke(idx)
      dut.io.write.bits.data.poke(data)
    }
    def unapplyWriteRequest(): Unit = dut.io.write.valid.poke(0.B)
    def applyReadRequest: Vec[UInt] = dut.io.source.request.index
    def getReadResponse: Vec[UInt] = dut.io.source.response.data
  }
}