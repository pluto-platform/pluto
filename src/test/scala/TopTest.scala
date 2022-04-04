

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class TopTest extends AnyFlatSpec with ChiselScalatestTester {



  "top" should "work" in {
    test(new Top).withAnnotations(Seq(WriteVcdAnnotation)) {dut =>
      dut.clock.setTimeout(0)
      dut.clock.step(1000)
    }
  }


}

