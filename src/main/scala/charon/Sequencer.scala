package charon

import charon.Charon.Link
import chisel3._
import chisel3.util.Counter
import lib.util.{BundleItemAssignment, ByteSplitter, SeqToVecMethods}
import peripherals.Leds
import charon.Charon.RangeBinder

class Sequencer extends Module {

  val io = IO(new Bundle {
    val tilelink = Tilelink.Agent.Interface.Requester(Tilelink.Parameters(4, 10, 2, Some(5), Some(5)))
    val error = Output(Bool())
  })

  val counter = RegInit(UInt(8.W), 0.U)

  val writeCounter = RegInit(0.B)

  when(io.tilelink.a.ready) {
    counter := counter + 1.U
    writeCounter := !writeCounter
  }


  io.tilelink.d.ready := 1.B

  val errorReg = RegInit(0.B)
  io.error := errorReg

  errorReg := io.tilelink.d.bits.denied && io.tilelink.d.valid

  io.tilelink.a.set(
    _.valid := 1.B,
    _.bits.set(
      _.opcode := Tilelink.Operation.PutFullData,
      _.param := 0.U,
      _.size := 2.U,
      _.source := 0.U,
      _.address := counter ## 0.U(2.W),
      _.mask := VecInit(1.B,1.B,1.B,1.B),
      _.data := writeCounter.asUInt.toBytes(4),
      _.corrupt := 0.B
    )
  )

}


class CharonTest extends Module {

  val io = IO(new Bundle {
    val leds = Output(UInt(4.W))
    val error = Output(Bool())
  })

  val sequencer = Seq.fill(3)(Module(new Sequencer))

  val leds = Seq.fill(4)(Module(new Leds(1)))

  Link(sequencer.map(_.io.tilelink), Seq(
    leds(0).io.tilelink.bind(0x00),
    leds(1).io.tilelink.bind(0x04),
    leds(2).io.tilelink.bind(0x08),
    leds(3).io.tilelink.bind(0x0C)
  ))

  io.leds := leds.map(_.io.leds).reduce(_ ## _)
  io.error := sequencer(0).io.error

}