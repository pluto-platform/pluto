package plutocore.pipeline

import lib.util.{ConcreteValueToOption, IntSeqToBigIntSeq}
import chisel3._
import chiseltest._
import ControlTypes.MemoryOperation
import plutocore.pipeline.PipelineTest.PipelineWrapper
import lib.util.BundleItemAssignment
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.{Files, Paths}

class PipelineTest extends AnyFlatSpec with ChiselScalatestTester {

  def pipelineTest(mem: Seq[BigInt], cycles: Int)(initialState: Pipeline.State, finalState: Pipeline.State): TestResult = {
    test(new Pipeline(initialState)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.io.instructionChannel.request.ready.poke(1.B)

      dut.io.dataChannel.request.ready.poke(1.B)
      dut.io.dataChannel.response.valid.poke(1.B)
      dut.io.dataChannel.response.bits.readData.poke(0xDEADBEEFL.U)

      var instrAddr = 0
      var cycleCount = 0

      while(mem(instrAddr) != 0x73) {
        instrAddr = dut.io.instructionChannel.request.bits.address.peek.litValue.toInt / 4
        dut.clock.step()
        dut.io.instructionChannel.response.valid.poke(1.B)
        dut.io.instructionChannel.response.bits.instruction.poke(mem(instrAddr).U)
        cycleCount += 1
      }
      for(_ <- 0 until 5) {
        dut.clock.step()
      }


      //assert(dut.getState == finalState)
      //assert(cycleCount == cycles)

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

  it should "add" in {
    val program = Files.readAllBytes(Paths.get("asm/loadUse.bin"))
      .map(_.toLong & 0xFF)
      .map(BigInt(_))
      .grouped(4)
      .map(a => a(0) | (a(1) << 8) | (a(2) << 16) | (a(3) << 24))
      .toArray
    pipelineTest(program, 6)(
      Pipeline.State(0, Seq.fill(32)(0)),
      Pipeline.State(5*4, Seq.fill(32)(0))
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
