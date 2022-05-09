package cache.data

import cache.Cache
import chisel3._
import chiseltest._
import cores.lib.ControlTypes.{MemoryAccessWidth, MemoryOperation}
import org.scalatest.flatspec.AnyFlatSpec

class DirectMappedSpec extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Direct-mapped data cache"

  it should "be nice" in {

    test(new DirectMapped(Cache.Dimension(
      1024, 16, BigInt(0) until BigInt(0x10000)
    ))).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.io.request.valid.poke(1.B)
      dut.io.request.bits.operation.poke(MemoryOperation.Write)
      dut.io.request.bits.size.poke(MemoryAccessWidth.HalfWord)

      dut.io.tilelink.d.bits.data.foreach(_.poke(0xAB.U))
      dut.io.request.bits.writeData.foreach(_.poke(0xEF.U))
      dut.io.tilelink.a.ready.poke(1.B)
      dut.io.tilelink.d.valid.poke(1.B)


      dut.clock.step()

      dut.io.request.bits.operation.poke(MemoryOperation.Read)
      dut.io.request.bits.size.poke(MemoryAccessWidth.Byte)

      while(!dut.io.request.ready.peek.litToBoolean) dut.clock.step()

      dut.io.request.bits.operation.poke(MemoryOperation.Read)
      dut.io.request.bits.size.poke(MemoryAccessWidth.HalfWord)

      dut.clock.step()

      dut.io.request.bits.operation.poke(MemoryOperation.Read)
      dut.io.request.bits.size.poke(MemoryAccessWidth.Word)

      dut.clock.step()


    }

  }

}
