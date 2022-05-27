package peripherals

import charon.Tilelink
import chisel3._
import chisel3.util.ShiftRegister
import lib.util.{BundleItemAssignment, ByteSplitter, rising, synchronize}

object Button {
  def apply(button: Bool): Button = {
    val mod = Module(new Button)
    mod.io.button := button
    mod
  }
}

class Button extends Module {

  val io = IO(new Bundle {

    val button = Input(Bool())
    val interrupt = Output(Bool())

    val tilelink = Tilelink.Agent.Interface.Responder(Tilelink.Parameters(4, 2, 2, Some(10), Some(10)))

  })

  val buttonInterruptReg = RegInit(0.B)

  io.interrupt := buttonInterruptReg

  when(rising(synchronize(io.button))) {
    buttonInterruptReg := 1.B
  }.elsewhen(io.tilelink.a.valid && io.tilelink.a.bits.opcode =/= Tilelink.Operation.Get) {
    buttonInterruptReg := io.tilelink.a.bits.data(0)(0)
  }

  io.tilelink.a.ready := 1.B
  io.tilelink.d.valid := RegNext(io.tilelink.a.valid, 0.B)
  io.tilelink.d.bits.set(
    _.opcode := Mux(io.tilelink.a.bits.opcode === Tilelink.Operation.Get, Tilelink.Response.AccessAckData, Tilelink.Response.AccessAck),
    _.param := 0.U,
    _.size := 2.U,
    _.source := RegNext(io.tilelink.a.bits.source, 0.U),
    _.sink := 0.U,
    _.denied := 0.B,
    _.data := buttonInterruptReg.asUInt.toBytes(4),
    _.corrupt := 0.B
  )

}
