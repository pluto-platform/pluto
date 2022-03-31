package cores.nix


import chisel3._
import chisel3.util._
import cores.nix.Pipeline.InstructionChannel
import lib.util.BundleItemAssignment

object ProgramCounter {

}

class ProgramCounter(init: Option[BigInt] = None) extends Module{

  val io = IO(new Bundle {
    val instructionRequest = new InstructionChannel.Request
    val value = Output(UInt(32.W))
    val stall = Input(Bool())
    val branching = new Branching.ProgramCounterChannel
    val exception = new ExceptionUnit.ProgramCounterChannel
  })

  val reg = RegInit(init.getOrElse(BigInt(0)).U(32.W))

  val nextReg = MuxCase(reg + 4.U, Seq(
    io.exception.jump -> io.exception.target,
    io.branching.jump -> io.branching.target,
    io.stall -> reg
  ))
  reg := nextReg

  io.instructionRequest.set(
    _.valid := 1.B,
    _.bits.address := nextReg
  )
  io.value := reg
}
