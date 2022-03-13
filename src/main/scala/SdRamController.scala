import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import lib.modules.TriStateDriver

object SdRamController {


  object State extends ChiselEnum {
    val Idle, Activate, WaitActivate, Read, Write, Precharge, WaitPrecharge, Refresh, WaitRefresh = Value
  }

  object Command extends ChiselEnum {
    val ModeRegisterSet = Value(0x00.U)
    val AutoOrSelfRefresh, Precharge, BankActivate, Write, Read, BurstStop, Nop = Value
    val DeviceDeselect = Value(0xF.U)
  }
}
import SdRamController.State
class SdRamController extends MultiIOModule {

  val backend = IO(new DE2_115.SDRAM)

  val io = IO(new Bundle {
    val addr = Input(UInt(15.W))
    val rdData = Output(UInt(32.W))
    val wrData = Input(UInt(32.W))
    val mask = Input(Vec(4,Bool()))
    val ready = Output(Bool())
    val we = Input(Bool())
  })


  val stateReg = RegInit(State.Idle)

  val driver = Module(new TriStateDriver(32))

  val counter = RegInit(0.U(5.W))


  switch(stateReg) {
    is(State.Idle) {
      stateReg := State.Idle
    }
    is(State.Activate) {

    }
    is(State.WaitActivate) {

    }
    is(State.Read) {

    }
    is(State.Write) {

    }
    is(State.Precharge) {

    }
    is(State.WaitPrecharge) {

    }
    is(State.Refresh) {

    }
    is(State.WaitRefresh) {

    }
  }

}
