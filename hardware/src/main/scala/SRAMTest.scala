import DE2_115._
import SRAMTest.State
import chisel3._
import chisel3.experimental.{Analog, ChiselEnum, attach}
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import chisel3.util._

import java.io.File
import scala.math.pow

object SRAMTest {
  object State extends ChiselEnum {
    val Idle, write, read, done, error, done2 = Value
  }
}


class SRAMTest extends Module {

  val io = IO(new Bundle {
    val address = Output(UInt(20.W))
    val data = Analog(16.W)
    val outputEnable = Output(Bool())
    val writeEnable = Output(Bool())
    val chipSelect = Output(Bool())
    val strobe = Output(Vec(2,Bool()))
    val writing = Output(Bool())
    val reading = Output(Bool())
    val done = Output(Bool())
    val error = Output(Bool())
  })

  withReset(!reset.asBool()) {

    val driver = Module(new TriStateDriver(16))

    val stateReg = RegInit(State.Idle)
    val counter = RegInit(0.U(20.W))

    attach(io.data, driver.io.bus)

    io.strobe := VecInit(0.B, 1.B)
    io.chipSelect := 0.B
    io.writeEnable := 1.B
    io.outputEnable := 1.B
    io.address := counter
    driver.io.drive := 0.B
    driver.io.driveData := counter
    val readVal = driver.io.busData

    io.writing := 0.B
    io.reading := 0.B
    io.done := 0.B
    io.error := 0.B

    switch(stateReg) {
      is(State.Idle) {
        io.done := 1.B
        io.error := 1.B
        stateReg := State.write
      }
      is(State.write) {
        stateReg := State.write
        io.writing := 1.B
        counter := counter + 1.U
        driver.io.drive := 1.B
        io.writeEnable := 0.B
        when(counter === (pow(2, 20).toInt - 1).U) {
          stateReg := State.read
          counter := 0.U
        }
      }
      is(State.read) {
        io.reading := 1.B
        counter := counter + 1.U
        io.outputEnable := 0.B
        stateReg := State.read
        when(counter === (pow(2, 20).toInt - 1).U) {
          stateReg := State.done
        }
        when(readVal(7, 0) =/= counter(7, 0)) {
          stateReg := State.error
        }
      }
      is(State.done) {
        stateReg := State.done2
        io.done := 1.B
      }
      is(State.error) {
        stateReg := State.error
        io.error := 1.B
      }
      is(State.done2) {
        stateReg := State.done
        io.done := 1.B
      }
    }

  }

}



class Quartus[M <: RawModule](mod: => M)(mapping: M => ((Data,String),Pin)*) extends App {

}

object SRAMTestGen extends Quartus(new SRAMTest)(
  _.io.error -> "io_error" -> LED.R(0),
  _.io.done -> "io_done" -> LED.G(0)
)
import sys.process._
object SRAMTest extends App {
  (new ChiselStage).emitVerilog(new SRAMTest, args = Array("--target-dir", "generated"))

  println(Process("/opt/intel/21.1/quartus/bin/quartus_sh -t setup_proj.tcl",new File("quartus")).!)
  println(Process("/opt/intel/21.1/quartus/bin/quartus_map SRAMTest",new File("quartus")).!)
  println(Process("/opt/intel/21.1/quartus/bin/quartus_fit SRAMTest",new File("quartus")).!)
  println(Process("/opt/intel/21.1/quartus/bin/quartus_asm SRAMTest",new File("quartus")).!)
  println(Process("/opt/intel/21.1/quartus/bin/quartus_pgm -c USB-Blaster -m jtag -o p;SRAMTest.sof",new File("quartus")).!)

}