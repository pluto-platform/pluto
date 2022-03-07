package core.pipeline

import lib.util.{ConcreteValueToOption, IntSeqToBigIntSeq}
import chisel3._
import chiseltest._
import core.ControlTypes.MemoryOperation
import core.pipeline.PipelineTest.PipelineWrapper
import core.{Pipeline, Top}
import lib.util.BundleItemAssignment
import org.scalatest.flatspec.AnyFlatSpec

class PipelineTest extends AnyFlatSpec with ChiselScalatestTester {

  def pipelineTest(mem: Seq[BigInt], cycles: Int)(initialState: Pipeline.State, finalState: Pipeline.State): TestResult = {
    test(new Pipeline(initialState)) { dut =>

      dut.io.instructionChannel.request.ready.poke(1.B)

      for(_ <- 0 until cycles) {
        val instrAddr = dut.io.instructionChannel.request.bits.address.peek.litValue.toInt
        val dataAddr = dut.io.dataChannel.request.bits.address.peek.litValue.toInt
        val isRead = dut.io.dataChannel.request.valid.peek.litToBoolean && dut.io.dataChannel.request.bits.op.peek == MemoryOperation.Read
        if (dut.io.dataChannel.request.valid.peek.litToBoolean && dut.io.dataChannel.request.bits.op.peek == MemoryOperation.Write) {

        }
        dut.clock.step()
        dut.io.instructionChannel.response.valid.poke(1.B)
        dut.io.instructionChannel.response.bits.instruction.poke(mem(instrAddr/4).U)
        //dut.io.dataChannel.response.bits.readData.poke(mem(dataAddr/4).U)
        dut.io.dataChannel.response.valid.poke(isRead.B)

      }

      assert(dut.getState == finalState)

    }
  }

  behavior of "Pipeline"

  it should "be nice" in {
    test(new Pipeline).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      val prog = Seq(
        0x08000093L
      ).map(_.U) ++ Seq.fill(100)(0x13.U)

      println(prog.map(_.litValue.toInt.toHexString))

      for(i <- 0 until 6) {
        dut.io.instructionChannel.request.ready.poke(1.B)
        val address = dut.io.instructionChannel.request.bits.address.peek.litValue.toInt
        dut.clock.step()
        dut.io.instructionChannel.set(
          _.response.valid.poke(1.B),
          _.response.bits.instruction.poke(prog(address/4))
        )

      }
      dut.clock.step()
    }
  }

  it should "work" in {
    test(new Top).withAnnotations(Seq(WriteVcdAnnotation)) {dut =>
      dut.clock.step(50)
    }
  }

  it should "add" in {
    pipelineTest(Seq(0x002081b3L, 0x13, 0x13, 0x13, 0x13, 0x13), 6)(
      Pipeline.State(0, Seq(0, 25, 26) ++ Seq.fill(29)(0)),
      Pipeline.State(5*4, Seq(0, 25, 26, 51) ++ Seq.fill(28)(0))
    )

  }




}

object PipelineTest {

  implicit class PipelineWrapper(dut: Pipeline) {
    def getState: Pipeline.State = {
      Pipeline.State(dut.simulation.get.pc.peek.litValue, dut.simulation.get.registerFile.map(_.peek.litValue))
    }
  }

}
