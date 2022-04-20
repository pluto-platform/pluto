package cores.modules

import chisel3._

class PipelineControl extends Bundle {
  val stall = Bool()
  val flush = Bool()
}

abstract class PipelineStage[UP <: Data, DOWN <: Data](up: => UP, down: => DOWN) extends Module {

  val upstream = IO(new Bundle {
    val reg = Input(up)
    val flowControl = Output(new PipelineControl)
  })
  val downstream = IO(new Bundle {
    val reg = Output(down)
    val flowControl = Input(new PipelineControl)
  })
  upstream.flowControl := downstream.flowControl

  def attachRegister(reg: PipelineRegister[DOWN]): PipelineRegister[DOWN] = {
    reg.upstream.data := downstream.reg
    downstream.flowControl := reg.upstream.flowControl
    reg
  }

}


object PipelineRegister {
  def apply[T <: Data](gen: => T): PipelineRegister[T] = Module(new PipelineRegister(gen))
}

class PipelineRegister[T <: Data](gen: => T) extends Module {
  val upstream = IO(new Bundle {
    val data = Input(gen)
    val flowControl = Output(new PipelineControl)
  })
  val downstream = IO(new Bundle {
    val data = Output(gen)
    val flowControl = Input(new PipelineControl)
  })
  val enable = IO(Input(Bool()))

  val reg = RegInit(0.U.asTypeOf(gen))
  when(enable) {reg := upstream.data}

  downstream.data := reg
  upstream.flowControl := downstream.flowControl

  def attachStage[S <: PipelineStage[T,_]](stage: S): S = {
    stage.upstream.reg := downstream.data
    downstream.flowControl := stage.upstream.flowControl
    enable := !stage.upstream.flowControl.stall
    stage
  }
}