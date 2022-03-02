package core.pipeline

import chisel3._
import chiseltest._
import core.Pipeline
import lib.util.BundleItemAssignment
import org.scalatest.flatspec.AnyFlatSpec

class PipelineTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Pipeline"

  it should "be nice" in {
    test(new Pipeline).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      val prog = Seq(
        0x08000093L
      ).map(_.U) ++ Seq.fill(100)(0x13.U)

      println(prog.map(_.litValue.toInt.toHexString))

      for(i <- 0 until 6) {
        dut.io.instructionChannel.request.ready.poke(1.B)
        val address = dut.io.instructionChannel.request.bits.address.peek.litValue.toInt
        dut.clock.step()
        dut.io.instructionChannel.set(
          _.response.valid.poke(1.B),
          _.response.bits.instruction.poke(prog(address/4))
        )

      }
      dut.clock.step()
    }
  }

  it should "work" in {
    test(new Top).withAnnotations(Seq(WriteVcdAnnotation)) {dut =>
      dut.clock.step(50)
    }
  }

}

object PipelineTest {


}
