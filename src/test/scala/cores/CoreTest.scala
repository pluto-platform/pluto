package cores

import chisel3._
import chiseltest._
import cores.nix.Nix
import org.scalatest.flatspec.AnyFlatSpec

class CoreTest extends AnyFlatSpec with ChiselScalatestTester {


  behavior of "Nix"

  it should "run" in {
    test(new Nix).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.instructionRequester.a.ready.poke(1.B)
      dut.io.instructionRequester.d.valid.poke(1.B)
      dut.io.instructionRequester.d.data(0).poke(0x13.U)
      dut.clock.step(50)
    }
  }
}
