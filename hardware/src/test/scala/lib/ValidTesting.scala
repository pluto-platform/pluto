package lib

import chisel3._
import chisel3.util.Valid
import chiseltest._

object ValidTesting {

  implicit class ValidPoker[T <: Data](bundle: Valid[T]) {
    def send(data: T): Unit = {
      bundle.valid.poke(1.B)
      bundle.bits.poke(data)
    }
    def choke(): Unit = {
      bundle.valid.poke(0.B)
    }
  }

}
