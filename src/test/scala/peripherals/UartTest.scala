package peripherals


import chisel3._
import chiseltest._
import lib.RandomHelper.uRands
import org.scalatest.flatspec.AnyFlatSpec
import peripherals.UartTest.UartPin
import peripherals.uart.{UartReceiver, UartTransmitter}

class UartTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "UART Receiver"

  it should "receive" in {

    test(new UartReceiver).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.io.period.poke(9.U)
      dut.resetLine()

      uRands(100, 8.W).foreach { v =>
        dut.transmit(v)

        dut.io.received.bits.expect(v)
      }

    }

  }

  behavior of "UART Transmitter"

  it should "transmit" in {
    test(new UartTransmitter).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.io.period.poke(9.U)

      dut.clock.step(2)

      dut.io.send.bits.poke(0x55.U)
      dut.io.send.valid.poke(1.B)
      dut.io.send.ready.expect(1.B)

      dut.clock.step()

      dut.io.send.valid.poke(0.B)

      while(!dut.io.send.ready.peek.litToBoolean) dut.clock.step()


      dut.io.send.bits.poke(0xFF.U)
      dut.io.send.valid.poke(1.B)
      dut.io.send.ready.expect(1.B)

      dut.clock.step()

      dut.io.send.valid.poke(0.B)

      while(!dut.io.send.ready.peek.litToBoolean) dut.clock.step()

      dut.io.send.bits.poke(0x0.U)
      dut.io.send.valid.poke(1.B)
      dut.io.send.ready.expect(1.B)

      dut.clock.step()

      dut.io.send.valid.poke(0.B)

      while(!dut.io.send.ready.peek.litToBoolean) dut.clock.step()

    }
  }

}

object UartTest {
  implicit class UartPin(dut: UartReceiver) {
    def resetLine(): Unit = {
      dut.io.rx.poke(1.B)
      dut.clock.step(2)
    }
    def transmit(x: UInt): Unit = {
      // start bit
      dut.io.rx.poke(0.B)
      dut.clock.step(dut.io.period.peek.litValue.toInt + 1)

      // message
      (0 until 8).foreach { i =>
        dut.io.rx.poke(((x.litValue >> i) & 1).B)
        dut.clock.step(dut.io.period.peek.litValue.toInt + 1)
      }

      // stop bit
      dut.io.rx.poke(1.B)
      dut.clock.step(dut.io.period.peek.litValue.toInt + 1)

    }
  }
}