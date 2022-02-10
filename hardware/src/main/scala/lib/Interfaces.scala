package lib

import chisel3._

object Interfaces {
  class Channel[REQ<: Data, RES <: Data](req: => REQ, res: => RES) extends Bundle {
    val request = Output(req)
    val response = Input(res)
  }
}
