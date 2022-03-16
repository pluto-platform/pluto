package lib

import chisel3._
import chiseltest.testableData

object BundleExpect {
  implicit class BundlerExpecter[T <: Bundle](x: T) {
    def expect(expects: T => (Data,Data)*): Unit = {
      expects.map(_(x)).foreach { case (port,value) => port.expect(value)}
    }
  }
}
