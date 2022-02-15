package core

import chisel3._
import chisel3.util.RegEnable

class PipelineControl extends Bundle {
  val stall = Bool()
  val flush = Bool()
}

abstract class PipelineStage[UP <: Data, DOWN <: Data](up: => UP, down: => DOWN) extends Module {

  val upstream = IO(Input(up))
  val downstream = IO(Output(down))
  val control = IO(new Bundle {
    val upstream = Output(new PipelineControl)
    val downstream = Input(new PipelineControl)
  })


  def :>(reg: PipelineRegister[DOWN]): PipelineRegister[DOWN] = {
    reg.upstream.data := downstream
    control.downstream := reg.upstream.control
    reg
  }
}

object PipelineRegister {
  def apply[T <: Data](gen: => T): PipelineRegister[T] = new PipelineRegister(gen)
}

class PipelineRegister[T <: Data](gen: => T) extends Module {
  val upstream = IO(new Bundle {
    val data = Input(gen)
    val control = Output(new PipelineControl)
  })
  val downstream = IO(new Bundle {
    val data = Output(gen)
    val control = Input(new PipelineControl)
  })
  val enable = IO(Input(Bool()))

  downstream.data := RegEnable(upstream.data, enable)
  downstream.control := upstream.control

  def |>[S <: PipelineStage[T,_]](stage: S): S = {
    enable := !stage.control.upstream.stall
    downstream.control := stage.control.upstream
    stage.upstream := downstream.data
    stage
  }
}