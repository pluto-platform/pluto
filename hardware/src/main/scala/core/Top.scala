package core

import chisel3._
import core.ControlTypes.{MemoryAccessResult, MemoryOperation}
import lib.modules.SyncROM
import lib.util.BundleItemAssignment

import java.nio.file.{Files, Paths}

class Top extends Module {
  val io = IO(new Bundle {
    val led = Output(Bool())
  })
  val pipeline = Module(new Pipeline)

  val simpleBlink = Files.readAllBytes(Paths.get("asm/blink.bin"))
    .map(_.toLong & 0xFF)
    .grouped(4)
    .map(a => a(0) | (a(1) << 8) | (a(2) << 16) | (a(3) << 24))
    .toArray

  val advancedBlink = Seq(
    0x100002b7L,
    0x00028293L,
    0x00000213L,
    0x00c000efL,
    0x01c000efL,
    0xff9ff06fL,
    0x00400193L,
    0x00000113L,
    0x00110113L,
    0xfe314ee3L,
    0x00008067L,
    0x0042a023L,
    0xfff24213L,
    0x00008067L,
  )

  val rom = SyncROM(simpleBlink.map(_.U(32.W)),simulation = true)

  rom.io.address := pipeline.io.instructionChannel.request.bits.address(31,2)
  pipeline.io.instructionChannel.set(
    _.request.ready := 1.B,
    _.response.valid := 1.B,
    _.response.bits.instruction := rom.io.data
  )
  pipeline.io.dataChannel.set(
    _.request.ready := 1.B,
    _.response.set(
      _.valid := 1.B,
      _.bits.set(
        _.readData := 0.U,
        _.result := MemoryAccessResult.Success
      )
    )
  )

  val ledReg = RegInit(0.B)

  when(pipeline.io.dataChannel.request.valid
    && pipeline.io.dataChannel.request.bits.op === MemoryOperation.Write
    && pipeline.io.dataChannel.request.bits.address === 0x10000000L.U) {
    ledReg := pipeline.io.dataChannel.request.bits.writeData(0)
  }

  io.led := ledReg

}

object TopEmitter extends App {
  emitVerilog(new Top, Array("--target-dir","build"))
}