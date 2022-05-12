package peripherals

import chisel3._
import chisel3.util.{Counter, MuxLookup}

class SevenSegmentDisplay(period: Int) extends Module {

  val io = IO(new Bundle {

    val number = Input(UInt(16.W))
    val an = Output(UInt(4.W))
    val seg = Output(UInt(7.W))
  })

  val decoder = VecInit(
    "b1000000".U,
    "b1111001".U,
    "b0100100".U,
    "b0110000".U,
    "b0011001".U,
    "b0010010".U,
    "b0000010".U,
    "b1111000".U,
    "b0000000".U,
    "b0010000".U,
    "b0001000".U,
    "b0000011".U,
    "b1000110".U,
    "b0100001".U,
    "b0000110".U,
    "b0001110".U
  )

  val counter = Counter(period)
  val anReg = RegInit("b0001".U(4.W))

  when(counter.inc()) {
    anReg := anReg(2,0) ## anReg(3)
  }
  io.an := ~anReg
  io.seg := decoder(MuxLookup(anReg, 0.U, Seq(
    "b0001".U -> io.number(3,0),
    "b0010".U -> io.number(7,4),
    "b0100".U -> io.number(11,8),
    "b1000".U -> io.number(15,12)
  )))

}
