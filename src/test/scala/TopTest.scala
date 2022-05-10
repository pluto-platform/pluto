
import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class TopTest extends AnyFlatSpec with ChiselScalatestTester {



  "top" should "ecall" in {
    test(new Top("asm/csr.bin")).withAnnotations(Seq(WriteVcdAnnotation)) {dut =>
      dut.clock.setTimeout(0)
      dut.clock.step(300)
    }
  }

  "top" should "interrupt" in {
    test(new Top("asm/interrupt.bin")).withAnnotations(Seq(WriteVcdAnnotation)) {dut =>
      dut.clock.setTimeout(0)
      dut.clock.step(21)
      dut.io.rx.poke(1.B)
      dut.clock.step(100)
    }
  }

  "top with cache" should "work" in {
    test(new TopCached).withAnnotations(Seq(WriteVcdAnnotation)) {dut =>
      dut.clock.setTimeout(0)
      dut.clock.step(1000)
    }
  }


}

