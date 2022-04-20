

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class TopTest extends AnyFlatSpec with ChiselScalatestTester {



  "top" should "work" in {
    test(new Top).withAnnotations(Seq(WriteVcdAnnotation,VerilatorBackendAnnotation)) {dut =>
      dut.clock.setTimeout(0)
      dut.clock.step(100000)
    }
  }

  "top with cache" should "work" in {
    test(new TopCached).withAnnotations(Seq(WriteVcdAnnotation)) {dut =>
      dut.clock.setTimeout(0)
      dut.clock.step(200)
    }
  }


}

