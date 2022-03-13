package plutocore.pipeline

import chisel3._
import chiseltest._
import ControlTypes.AluFunction
import org.scalatest.flatspec.AnyFlatSpec
import lib.RandomHelper._

class AluSpec extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "ALU"

  it should "add" in {
    test(new ALU) { dut =>
      val vs = uRands(32.W,32.W)
      dut.io.operation.poke(AluFunction.Addition)
      dut.io.operand.zip(vs).foreach { case (o,v) => o.poke(v)}
      dut.io.result.expect(vs.map(_.litValue).sum.U)
    }
  }

}
