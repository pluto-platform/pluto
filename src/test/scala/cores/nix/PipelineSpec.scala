package cores.nix

import chisel3._
import chiseltest._
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryOperation}
import cores.nix.Pipeline.State
import cores.nix.PipelineSpec.Memory
import lib.riscv.Assemble
import lib.riscv.Assemble.assembleToBytes
import org.scalatest.flatspec.AnyFlatSpec
import lib.util.{ConcreteValueToOption, IntSeqToBigIntSeq, IntToBigInt}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class PipelineSpec extends AnyFlatSpec with ChiselScalatestTester {


  behavior of "Nix Pipeline"

  def pipelineTest(instructions: Seq[String], memory: Seq[(Int,Int)])(pcChange: (Long, Long))(regChange: Map[Int,(Long,Long)]): TestResult = {

    val regs = Seq.tabulate(32)(i => BigInt(regChange.getOrElse(i, (0L,0L))._1))
    val initialState = State(pcChange._1, regs)

    test(new Pipeline(initialState)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      val mem = new Memory(dut)(assembleToBytes(instructions).zipWithIndex.map(t => (t._2+pcChange._1.toInt,t._1)) ++ memory)

      instructions.foreach { _ =>
        mem.step()
        dut.clock.step()
      }
      (0 until 5).foreach { _ =>
        mem.step()
        dut.clock.step()
      }

    }
  }

  it should "add" in {
    pipelineTest(Seq("add x1, x2, x3"), Seq())(20L -> 24L)(Map(
      1 -> (23,45),
      2 -> (19,19),
      3 -> (26,26)
    ))
  }
  it should "branch" in {
    pipelineTest(Seq(
      "top:",
      "addi x1, x0, 0x18",
      "addi x2, x0, 0x20",
      "blt x1, x2, top",
      "li x1, 0x100",
      "li x2, 0x200"
    ), Seq())(20L -> 24L)(Map(
      1 -> (23,45),
      2 -> (19,19),
      3 -> (26,26)
    ))
  }


}

object PipelineSpec {

  class Memory(dut: Pipeline)(init: Seq[(Int,Int)]) {

    val mem = ArrayBuffer.fill(1024)(0)
    println(init,init.map(_._1).max, mem.length, mem.size)
    init.foreach { case (address, value) => mem(address) = value }

    var instrRequestPipe = false
    var instrReadPipe = 0

    var dataRequestPipe = false
    var dataReadWidth = 0
    var dataReadPipe = 0
    var writeSuccesPipe = false

    dut.io.instructionChannel.request.ready.poke(1.B)
    dut.io.dataChannel.request.ready.poke(1.B)

    def step(): Unit = {
      println(instrRequestPipe, instrReadPipe)
      dut.io.instructionChannel.response.valid.poke(instrRequestPipe.B)
      dut.io.instructionChannel.response.bits.instruction.poke(0.U)
      if(instrRequestPipe) {
        if (instrReadPipe % 4 != 0) throw new Exception("Unaligned instruction access")
        val instr = mem(instrReadPipe).toLong | (mem(instrReadPipe + 1).toLong << 8)  | (mem(instrReadPipe + 2).toLong << 16)  | (mem(instrReadPipe + 3).toLong << 24)
        dut.io.instructionChannel.response.bits.instruction.poke(instr.U)
      }
      instrRequestPipe = dut.io.instructionChannel.request.valid.peek.litToBoolean
      instrReadPipe = dut.io.instructionChannel.request.bits.address.peek.litValue.toInt

      /*
      dut.io.dataChannel.response.valid.poke(dataRequestPipe.B)
      dut.io.dataChannel.response.bits.readData.poke(0.U)
      dut.io.dataChannel.response.bits.result.poke(if(dataRequestPipe || writeSuccesPipe) MemoryAccessResult.Success else MemoryAccessResult.Failure)
      if(dataRequestPipe) {
        val readData = dataReadWidth match {
          case 0 => mem(dataReadPipe).toLong
          case 1 => mem(dataReadPipe).toLong | (mem(dataReadPipe + 1) << 8).toLong
          case 2 => mem(dataReadPipe).toLong | (mem(dataReadPipe + 1) << 8).toLong  | (mem(dataReadPipe + 2) << 16).toLong  | (mem(dataReadPipe + 3) << 24).toLong
        }
        dut.io.dataChannel.response.bits.readData.poke(readData.U)
      }
      dataRequestPipe = dut.io.dataChannel.request.valid.peek.litToBoolean && (dut.io.dataChannel.request.bits.op.peek.litValue == 0)
      dataReadWidth = dut.io.dataChannel.request.bits.accessWidth.peek.litValue.toInt
      dataReadPipe = dut.io.dataChannel.request.bits.address.peek.litValue.toInt

      if(dut.io.dataChannel.request.valid.peek.litToBoolean && (dut.io.dataChannel.request.bits.op.peek.litValue == 1)) {
        writeSuccesPipe = true
        val addr = dut.io.dataChannel.request.bits.address.peek.litValue.toInt
        val wrData = dut.io.dataChannel.request.bits.writeData.peek.litValue.toLong
        val width = dut.io.dataChannel.request.bits.accessWidth.peek.litValue.toInt
        mem(addr) = (wrData & 0xFF).toInt
        if(width >= 1) {
          mem(addr + 1) = ((wrData >> 8) & 0xFF).toInt
        }
        if(width >= 2) {
          mem(addr + 2) = ((wrData >> 16) & 0xFF).toInt
          mem(addr + 3) = ((wrData >> 24) & 0xFF).toInt
        }
      } else writeSuccesPipe = false
      */
    }

    def step(n: Int): Unit = for(_ <- 0 until n) step()

  }

}
