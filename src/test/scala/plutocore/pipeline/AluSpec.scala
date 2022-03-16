package plutocore.pipeline

import chisel3._
import chiseltest._
import ControlTypes.AluFunction
import org.scalatest.flatspec.AnyFlatSpec
import lib.RandomHelper._

class AluSpec extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "ALU"

  Map[AluFunction.Type, (Int, Int) => Int](
    AluFunction.Addition -> (_ + _),
    AluFunction.Subtraction -> (_ - _),
    AluFunction.ShiftLeft -> (_ << _),
    AluFunction.Xor -> (_ ^ _)
  )

  def aluTest(op: AluFunction.Type)(behavior: (Int,Int) => Int): TestResult = {
    test(new ALU) { dut =>
      dut.io.operation.poke(op)
      Seq.fill(100)(uRands(32.W,32.W)).foreach { vs =>
        println(vs)
        dut.io.operand.zip(vs).foreach { case (port,v) => port.poke(v)}
        println(vs(0).litValue.intValue,vs(1).litValue.intValue)
        println((vs(0).litValue.intValue-vs(1).litValue.intValue).toLong & 0xFFFFFFFFL)
        println((behavior(vs(0).litValue.intValue, vs(1).litValue.intValue).toLong & 0xFFFFFFFFL))
        println((BigInt(behavior(vs(0).litValue.intValue, vs(1).litValue.intValue)) & 0xFFFFFFFFL))
        println(dut.io.operand(0).peek.litValue, dut.io.operand(1).peek.litValue, dut.io.result.peek.litValue)
        dut.io.result.expect((BigInt(behavior(vs(0).litValue.intValue, vs(1).litValue.intValue)) & 0xFFFFFFFFL).U)
      }
    }
  }

  it should "add" in aluTest(AluFunction.Addition)(_ + _)

  it should "subtract" in aluTest(AluFunction.Addition)(_ - _)


}
