package cores.nix

import chisel3._
import chiseltest._
import chiseltest.internal.Context
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryAccessWidth, MemoryOperation}
import cores.nix.Pipeline.State
import cores.nix.PipelineSpec.Memory
import lib.ChiselEnumPeek.ChiselEnumPeeker
import lib.riscv.Assemble
import lib.riscv.Assemble.assembleToBytes
import org.scalatest.flatspec.AnyFlatSpec
import lib.util.{ConcreteValueToOption, IntSeqToBigIntSeq, IntToBigInt}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class PipelineSpec extends AnyFlatSpec with ChiselScalatestTester {


  behavior of "Nix Pipeline"

  def pipelineTest(instructions: Seq[String], memory: Map[Int, (Int,Int)])(pcChange: (Long, Long))(regChange: Map[Int,(Long,Long)]): TestResult = {

    val regs = Seq.tabulate(32)(i => BigInt(regChange.getOrElse(i, (0L,0L))._1))
    val initialState = State(pcChange._1, regs)

    test(new Pipeline(initialState)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      val mem = new Memory(dut)(
        assembleToBytes(instructions).zipWithIndex.map(t => (t._2+pcChange._1.toInt,t._1))
          ++ memory.map { case (idx, (init,_)) =>
          idx -> init
        })

      while(!dut.simulation.get.isEcall.peek.litToBoolean) {
        mem.step()
        dut.clock.step()
      }

      regChange.foreach { case (i,(_,v)) =>
        dut.simulation.get.registerFile(i).expect(v.U)
      }

    }
  }

  it should "add" in {
    pipelineTest(Seq("add x1, x2, x3","ecall"), Map())(20L -> 24L)(Map(
      1 -> (23,45),
      2 -> (19,19),
      3 -> (26,26)
    ))
  }
  it should "branch" in {
    pipelineTest(Seq(
      "blt x1, x2, exit",
      "li x1, 0x100",
      "li x2, 0x200",
      "exit: ecall"
    ), Map())(20L -> 32L)(Map(
      1 -> (23,23),
      2 -> (24,24)
    ))
  }
  it should "not branch" in {
    pipelineTest(Seq(
      "blt x1, x2, exit",
      "li x1, 0x100",
      "li x2, 0x200",
      "exit: ecall"
    ), Map())(20L -> 32L)(Map(
      1 -> (25,0x100),
      2 -> (24,0x200)
    ))
  }

  it should "jump and link" in {
    pipelineTest(Seq(
      "jal x1, fun",
      "add x4, x2, x3",
      "ecall",
      "nop",
      "nop",
      "fun:",
      "li x2, 30",
      "jalr x5, 0(x1)",
      "li x2, 0"
    ), Map())(16L -> 24L)(Map(
      1 -> (0, 20),
      2 -> (10,30),
      3 -> (50,50),
      4 -> (0, 80),
      5 -> (0,44)
    ))
  }

  it should "load" in {
    pipelineTest(Seq(
      "lw x1, 0x40(x0)",
      "ecall"
    ), Map(
      0x40 -> (0xEF, 0xEF),
      0x41 -> (0xBE, 0xBE),
      0x42 -> (0xAD, 0xAD),
      0x43 -> (0xDE, 0xDE)
    ))(0x20L -> 0x24L)(Map(
      1 -> (3,0xDEADBEEFL)
    ))
  }

  it should "store" in {
    pipelineTest(Seq(
      "sw x3, 0x40(x2)",
      "ecall"
    ), Map(
      (0x123123+0x40) -> (0,0xEF),
      (0x123123+0x40+1) -> (0,0xBE),
      (0x123123+0x40+2) -> (0,0xEF),
      (0x123123+0x40+3) -> (0,0xBE)
    ))(0x40L -> 0x44L)(Map(
      2 -> (0x123123L, 0x123123L),
      3 -> (0xBEEFBEEFL,0xBEEFBEEFL)
    ))
  }


}

object PipelineSpec {

  class Memory(dut: Pipeline)(init: Seq[(Int,Int)]) {

    println(init)
    val mem = ArrayBuffer.fill(init.map(_._1).max+100)(0)
    init.foreach { case (address, value) => mem(address) = value }

    var instrRequestPipe = false
    var instrReadPipe = 0

    var dataRequestPipe = false
    var dataReadWidth = MemoryAccessWidth.Byte
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

      dut.io.dataChannel.response.valid.poke(dataRequestPipe.B)
      dut.io.dataChannel.response.bits.readData.poke(0.U)
      dut.io.dataChannel.response.bits.result.poke(if(dataRequestPipe || writeSuccesPipe) MemoryAccessResult.Success else MemoryAccessResult.Failure)
      if(dataRequestPipe) {
        val readData = dataReadWidth match {
          case MemoryAccessWidth.Byte => mem(dataReadPipe).toLong
          case MemoryAccessWidth.HalfWord => mem(dataReadPipe).toLong | (mem(dataReadPipe + 1).toLong << 8)
          case MemoryAccessWidth.Word => mem(dataReadPipe).toLong | (mem(dataReadPipe + 1).toLong << 8)  | (mem(dataReadPipe + 2).toLong << 16)  | (mem(dataReadPipe + 3).toLong << 24)
        }
        dut.io.dataChannel.response.bits.readData.poke(readData.U)
      }
      dataRequestPipe = dut.io.dataChannel.request.valid.peek.litToBoolean && (MemoryOperation.peek(dut.io.dataChannel.request.bits.op) == MemoryOperation.Read)
      if(dataRequestPipe) {
        dataReadWidth = MemoryAccessWidth.peek(dut.io.dataChannel.request.bits.accessWidth)
        dataReadPipe = dut.io.dataChannel.request.bits.address.peek.litValue.toInt
      }


      if(dut.io.dataChannel.request.valid.peek.litToBoolean && (MemoryOperation.peek(dut.io.dataChannel.request.bits.op) == MemoryOperation.Write)) {
        writeSuccesPipe = true
        val addr = dut.io.dataChannel.request.bits.address.peek.litValue.toInt
        val wrData = dut.io.dataChannel.request.bits.writeData.peek.litValue.toLong
        val width = MemoryAccessWidth.peek(dut.io.dataChannel.request.bits.accessWidth).litValue.toInt
        mem(addr) = (wrData & 0xFF).toInt
        if(width >= 1) {
          mem(addr + 1) = ((wrData >> 8) & 0xFF).toInt
        }
        if(width >= 2) {
          mem(addr + 2) = ((wrData >> 16) & 0xFF).toInt
          mem(addr + 3) = ((wrData >> 24) & 0xFF).toInt
        }
      } else writeSuccesPipe = false

    }



    def step(n: Int): Unit = for(_ <- 0 until n) step()

    def apply(i: Int): Int = mem(i)

  }

}
