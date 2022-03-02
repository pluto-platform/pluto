package core.pipeline

import chisel3._
import chiseltest._
import lib.modules.SyncROM
import org.scalatest.flatspec.AnyFlatSpec

class BBRomSpec extends AnyFlatSpec with ChiselScalatestTester {


  "ROM" should "be nice" in {
    test(new SyncROM(Seq.range(0,1024).map(_.U(32.W)),simulation = false))
      .withAnnotations(Seq(VerilatorBackendAnnotation)) { dut =>
      for(i <- 0 until 1024) {
        dut.io.address.poke(i.U)
        dut.clock.step()
        println(dut.io.data.peek().litValue)
      }

    }
  }

}
