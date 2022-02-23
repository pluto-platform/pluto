package core.pipeline

import chisel3._
import core.Pipeline.InstructionChannel
import lib.util.BundleItemAssignment

class ProgramCounter extends Module{

  val io = IO(new Bundle {
    val instructionRequest = new InstructionChannel.Request
    val value = Output(UInt(32.W))
  })

  val reg = RegInit(0.U(32.W))

  io.instructionRequest.set(
    _.valid := 1.B,
    _.bits.address := reg
  )
}
