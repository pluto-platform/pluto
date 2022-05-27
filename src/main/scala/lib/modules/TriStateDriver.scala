package lib.modules

import chisel3._
import chisel3.experimental.{Analog, attach}
import chisel3.util.{HasBlackBoxInline, Valid, ValidIO}

object TriStateDriver {
  def apply(wire: Analog): (UInt, ValidIO[UInt]) = {
    val mod = Module(new TriStateDriver(wire.getWidth))
    attach(wire, mod.io.bus)
    val driveInterface = Wire(Valid(UInt(wire.getWidth.W)))
    mod.io.drive := driveInterface.valid
    mod.io.driveData := driveInterface.bits
    (mod.io.busData, driveInterface)
  }
}

/**
 * This module allows to connect to tri-state busses with Chisel by using a Verilog blackbox.
 * @param width The width of the tri-state bus
 */
class TriStateDriver(width: Int) extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle{
    val busData =     Output(UInt(width.W))   // data on the bus
    val driveData =   Input(UInt(width.W))    // data put on the bus if io.drive is asserted
    val bus =         Analog(width.W)         // the tri-state bus
    val drive =       Input(Bool())           // when asserted the module drives the bus
  })

  setInline("TriStateDriver.v",
    s"""
       |module TriStateDriver(
       |    output [${width-1}:0] busData,
       |    input [${width-1}:0] driveData,
       |    inout [${width-1}:0] bus,
       |    input drive);
       |
       |    assign bus = drive ? driveData : {($width){1'bz}};
       |    assign busData = bus;
       |endmodule
       |""".stripMargin
  )
}