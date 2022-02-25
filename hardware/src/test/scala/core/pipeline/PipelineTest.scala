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
        0x02000093L,
        0x04000113L,
        0x401101b3L,
        0xfe309ae3L,
        0xff1ff26fL
      ).map(_.U) ++ Seq.fill(100)(0x13.U)

      println(prog.map(_.litValue.toInt.toHexString))

      for(i <- 0 until 2) {
        dut.io.instructionChannel.request.ready.poke(1.B)
        val address = dut.io.instructionChannel.request.bits.address.peek.litValue.toInt
        dut.clock.step()
        dut.io.instructionChannel.set(
          _.response.valid.poke(1.B),
          _.response.bits.instruction.poke(prog(address))
        )

      }
      dut.clock.step()
    }
  }

}
