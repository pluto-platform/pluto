package core.pipeline.stages

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class FetchSpec extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Fetch Stage"

  it should "signal to branch predictor to guess" in {
    test(new Fetch) { dut =>
      dut.upstream.data.pc.poke(0.U)
      dut.io.instructionResponse.valid.poke(1.B)
      dut.io.instructionResponse.bits.instruction.poke(0x00820063L.U)

      dut.io.branching.takeGuess.expect(1.B)

      dut.io.instructionResponse.bits.instruction.poke(0x13.U)

      dut.io.branching.takeGuess.expect(0.B)
    }
  }

  it should "detect a branch" in {
    test(new Fetch) { dut =>
      dut.upstream.data.pc.poke(0.U)
      dut.io.instructionResponse.valid.poke(1.B)
      dut.io.instructionResponse.bits.instruction.poke(0x00820063L.U)

      dut.io.branching.takeGuess.expect(1.B)
      dut.io.branching.jump.expect(0.B)
    }
  }

  it should "calculate the branch target" in {
    test(new Fetch) { dut =>
      dut.upstream.data.pc.poke(16.U)
      dut.io.instructionResponse.valid.poke(1.B)
      dut.io.instructionResponse.bits.instruction.poke(0xfe8208e3L.U)


      dut.io.branching.target.expect(0.U)
    }
  }

  it should "detect a jal" in {
    test(new Fetch) { dut =>
      dut.upstream.data.pc.poke(0.U)
      dut.io.instructionResponse.valid.poke(1.B)
      dut.io.instructionResponse.bits.instruction.poke(0xff1ff0efL.U)

      dut.io.branching.takeGuess.expect(0.B)
      dut.io.branching.jump.expect(1.B)
    }
  }

  it should "calculate the jal target" in {
    test(new Fetch) { dut =>
      dut.upstream.data.pc.poke(16.U)
      dut.io.instructionResponse.valid.poke(1.B)
      dut.io.instructionResponse.bits.instruction.poke(0xff1ff0efL.U)

      dut.io.branching.target.expect(0.U)
    }
  }

}
