package core.pipeline

import chisel3._
import chiseltest._
import core.pipeline.stages.Fetch
import org.scalatest.flatspec.AnyFlatSpec

class FetchSpec extends AnyFlatSpec with ChiselScalatestTester{

  behavior of "Fetch Stage"

  it should "be nice" in {
    test(new Fetch).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.upstream.data.pc.poke(0xDEADBEEFL.U)
      dut.io.instructionResponse.valid.poke(0.B)
      dut.io.instructionResponse.bits.instruction.poke(0x0e542fa3L.U)
      dut.clock.step(10)
      dut.io.instructionResponse.valid.poke(1.B)
      dut.clock.step(10)
    }
  }

}
