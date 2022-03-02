package lib.modules

import chisel3._
import chisel3.util.{HasBlackBoxInline, log2Ceil}
import firrtl.ir.Type
import firrtl_interpreter.{BlackBoxFactory, BlackBoxImplementation, Concrete, TypeInstanceFactory}
import treadle.{ScalaBlackBox, ScalaBlackBoxFactory}

object SyncROM {
  def apply[T <: Data](table: Seq[T], simulation: Boolean = false): SyncROM[T] = Module(new SyncROM(table,simulation))
}

class SyncROM[T <: Data](table: Seq[T], simulation: Boolean = false) extends Module {
  println(table)
  val io = IO(new Bundle {
    val address = Input(UInt(log2Ceil(table.length).W))
    val data = Output(chiselTypeOf(table.head))
  })
  if(simulation) {
    val rom = VecInit(table)
    io.data := rom(RegNext(io.address, 0.U))
  } else {
    val rom = Module(new BlackBoxSyncROM(table.map(_.asUInt.litValue)))
    rom.io.clock := clock
    rom.io.address := io.address
    io.data := rom.io.data.asTypeOf(table.head)
  }
}

class BlackBoxSyncROM(program: Seq[BigInt]) extends BlackBox with HasBlackBoxInline  {
  val io = IO(new Bundle{
    val clock = Input(Clock())
    val address = Input(UInt(32.W))
    val data = Output(UInt(32.W))
  })

  val romStr = program.zipWithIndex.map { case (data,index) =>
    s"\t\t31'h%08X: data <= 32'h%08X;".format(index,data)
  }.mkString("\n")


  setInline("BlackBoxSyncROM.v",
    s"""
       |module BlackBoxSyncROM(
       |    input clock,
       |    input [31:0] address,
       |    output reg [31:0] data
       |);
       |reg [29:0] addrReg;
       |
       |always @(posedge clock) begin
       |  addrReg <= address[31:2];
       |end
       |
       |always @(addrReg) begin
       |  case(addrReg)
       |$romStr
       |  endcase
       |end
       |
       |endmodule
       |""".stripMargin
  )

  def read(address: UInt): UInt = {
    0.U
  }
}

object EmitterROM extends App {
  emitVerilog(new SyncROM(Seq.range(0,1024).map(_.U(32.W))))
}