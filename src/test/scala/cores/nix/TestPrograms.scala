package cores.nix

import chisel3._
import chiseltest._
import cores.lib.ControlTypes.{MemoryAccessResult, MemoryAccessWidth, MemoryOperation}
import cores.nix.TestPrograms.Memory
import lib.Binaries
import org.scalatest.flatspec.AnyFlatSpec
import lib.ChiselEnumPeek.ChiselEnumPeeker

import scala.collection.mutable.ArrayBuffer

class TestPrograms extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Pipeline"

  val names = Binaries.getFiles("tests").filter(_.endsWith(".bin")).map(_.dropRight(4)).sorted
  println(names)
  val tests = names.map { name =>
    (name, Binaries.loadBytes(s"tests/$name.bin"), Binaries.loadWords(s"tests/$name.res"))
  }

  tests.foreach(testProgram)


  def testProgram(testTuple: (String,Seq[Int],Seq[BigInt])): Unit = {
    it should s"run ${testTuple._1}" in {
      test(new Pipeline(Some(Pipeline.State(0,Seq.fill(32)(BigInt(0)))))).withAnnotations(Seq(WriteVcdAnnotation,VerilatorBackendAnnotation)) { dut =>
        dut.clock.setTimeout(100)
        val mem = new Memory(dut)(0x100000,testTuple._2)

        while(!dut.simulation.get.isEcall.peek.litToBoolean) {
          mem.step()
          //println(dut.io.instructionChannel.response.bits.instruction.peek.litValue.toString(16))
          dut.clock.step()
        }

        testTuple._3.zip(dut.simulation.get.registerFile).foreach { case (should,port) =>
          port.expect(should.U)
        }

      }
    }
  }

}


object TestPrograms {

  class Memory(dut: Pipeline)(size: Int, init: Seq[Int]) {

    println(init.toList)
    val mem = ArrayBuffer.fill(size)(0)
    init.zipWithIndex.foreach { case (value, address) => mem(address) = value }

    var instrRequestPipe = false
    var instrReadPipe = 0

    var dataRequestPipe = false
    var dataReadWidth = MemoryAccessWidth.Byte
    var dataReadPipe = 0
    var writeSuccesPipe = false

    dut.io.instructionChannel.request.ready.poke(1.B)
    dut.io.dataChannel.request.ready.poke(1.B)

    def step(): Unit = {
      dut.io.instructionChannel.response.valid.poke(instrRequestPipe.B)
      dut.io.instructionChannel.response.bits.instruction.poke(0.U)
      if(instrRequestPipe) {
        if (instrReadPipe % 4 != 0) throw new Exception("Unaligned instruction access")
        val instr = mem(instrReadPipe).toLong | (mem(instrReadPipe + 1).toLong << 8)  | (mem(instrReadPipe + 2).toLong << 16)  | (mem(instrReadPipe + 3).toLong << 24)
        //println(s"$instrReadPipe -> ${instr.toHexString}")
        dut.io.instructionChannel.response.bits.instruction.poke(instr.U)
      }

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
      instrRequestPipe = dut.io.instructionChannel.request.valid.peek.litToBoolean
      instrReadPipe = dut.io.instructionChannel.request.bits.address.peek.litValue.toInt
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