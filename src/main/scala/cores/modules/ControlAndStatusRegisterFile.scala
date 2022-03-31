package cores.modules

import chisel3._
import chisel3.util._
import lib.util.BundleItemAssignment
import cores.lib.ControlAndStatusRegister.{Interrupts, PerformanceCounterControl, TrapMode}
import cores.lib.Exception

object ControlAndStatusRegisterFile {

  class WriteRequest extends Bundle {
    val index = UInt(12.W)
    val value = UInt(32.W)
  }
  class ReadRequest extends Bundle {
    val index = UInt(12.W)
  }
  class ReadResponse extends Bundle {
    val value = UInt(32.W)
  }


}

class ControlAndStatusRegisterFile extends Module {

  val io = IO(new Bundle {
    val readRequest = Flipped(Valid(new ControlAndStatusRegisterFile.ReadRequest))
    val readResponse = Output(new ControlAndStatusRegisterFile.ReadResponse)
    val writeRequest = Flipped(Valid(new ControlAndStatusRegisterFile.WriteRequest))
    val instructionRetired = Input(Bool())
    val timerEnable = Output(Bool())
    val exceptionUnit = new Exception.CSRChannel
  })



  val interruptEnableReg = RegInit(0.U.asTypeOf(new Interrupts))
  val interruptPendingReg = RegInit(0.U.asTypeOf(new Interrupts))

  val trapAddressReg = RegInit(0.U(30.W))
  val trapModeReg = RegInit(TrapMode.Direct)

  val counterEnableReg = RegInit(0.U.asTypeOf(new PerformanceCounterControl))
  val counterInhibitReg = RegInit(0.U.asTypeOf(new PerformanceCounterControl))

  val scratchRegister = RegInit(0.U(32.W))

  val exceptionPc = RegInit(0.U(32.W))
  val exceptionCause = RegInit(Exception.Cause.None)
  val exceptionValue = RegInit(0.U(32.W))


  val cycleCounter = RegInit(0.U(64.W))
  when(counterEnableReg.cycle && !counterInhibitReg.cycle) { cycleCounter := cycleCounter + 1.U }

  val instructionRetiredCounter = RegInit(0.U(64.W))
  when(counterEnableReg.instructionsRetired && !counterInhibitReg.instructionsRetired && io.instructionRetired) { instructionRetiredCounter := instructionRetiredCounter + 1.U }

  io.timerEnable := counterEnableReg.timer
  io.exceptionUnit.set(
    _.interruptEnable := interruptEnableReg
  )

  io.readResponse.value := RegNext(MuxLookup(io.readRequest.bits.index, 0.U, Seq(
    0xF11.U -> 0.U, // mvendorid: Vendor ID
    0xF12.U -> 0.U, // marchid: architecture ID
    0xF13.U -> 0.U, // mimpid: implementation ID
    0xF14.U -> 0.U, // mhartid: hardware thread ID
    0xF15.U -> 0.U, // mconfigptr: pointer to configuration data structure
    0x300.U -> interruptEnableReg.global ## 0.U(3.W), // mstatus: machine status register
    0x301.U -> 1.U(2.W) ## 0.U(4.W) ## 0.U(17.W) ## 1.B ## 0.U(8.W), // misa: ISA and extenstions
    0x304.U -> interruptEnableReg.custom.asUInt ## 0.U(4.W) ## interruptEnableReg.external ## 0.U(3.W) ## interruptEnableReg.timer ## 0.U(3.W) ## interruptEnableReg.software ## 0.U(3.W), // mie: machine interrupt-enable register
    0x305.U -> trapAddressReg ## trapModeReg.asUInt, // mtvec: machine trap-handler base address and mode
    0x306.U -> counterEnableReg.instructionsRetired ## counterEnableReg.timer ## counterEnableReg.cycle, // mcounteren: machine counter enable
    0x320.U -> counterInhibitReg.instructionsRetired ## 0.B ## counterInhibitReg.cycle, // mcounterinhibit: machine counter-inhibit register
    0x340.U -> scratchRegister, // mscratch: scratch register for machine trap handlers
    0x341.U -> exceptionPc, // mepc: machine exception program counter
    0x342.U -> exceptionValue, // mtval: machine bad address or instruction
    0xB00.U -> cycleCounter(31,0), // mcycle: machine cycle counter
    0xB02.U -> instructionRetiredCounter(31,0), // minstret: machine instructions-retired counter
    0xB80.U -> cycleCounter(63,32), // mcycleh: upper 32 bits of mcycle
    0xB82.U -> instructionRetiredCounter(63,32), // minstreth: upper 32 bits of minstret
  )))

  val writeValue = io.writeRequest.bits.value
  when(io.writeRequest.valid) {
    switch(io.writeRequest.bits.index) {
      is(0x300.U) {
        interruptEnableReg.global := writeValue(3)
      }
      is(0x304.U) {
        interruptEnableReg.set(
          _.custom := writeValue(31,16).asBools,
          _.external := writeValue(11),
          _.timer := writeValue(7),
          _.software := writeValue(3)
        )
      }
      is(0x305.U) {
        trapModeReg := Mux(writeValue(0), TrapMode.Vectored, TrapMode.Direct)
        trapAddressReg := writeValue(31,2)
      }
      is(306.U) {
        counterEnableReg.set(
          _.cycle := writeValue(0),
          _.timer := writeValue(1),
          _.instructionsRetired := writeValue(2)
        )
      }
      is(0x320.U) {
        counterInhibitReg.set(
          _.cycle := writeValue(0),
          _.instructionsRetired := writeValue(2)
        )
      }
      is(0x340.U) {
        scratchRegister := writeValue
      }
      is(0x341.U) {
        exceptionPc := writeValue
      }
      is(0x342.U) {
        exceptionValue := writeValue
      }
      is(0xB00.U) {
        cycleCounter := cycleCounter(63,32) ## writeValue
      }
      is(0xB02.U) {
        instructionRetiredCounter := instructionRetiredCounter(63,32) ## writeValue
      }
      is(0xB80.U) {
        cycleCounter := writeValue ## cycleCounter(31,0)
      }
      is(0xB82.U) {
        instructionRetiredCounter := writeValue ## instructionRetiredCounter(31,0)
      }
    }
  }

}
