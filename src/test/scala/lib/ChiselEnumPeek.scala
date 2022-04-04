package lib

import chisel3.experimental.{ChiselEnum, EnumType}
import chiseltest.internal.Context

object ChiselEnumPeek {

  implicit class ChiselEnumPeeker[T <: ChiselEnum](val enum: T) {
    def peek(x: enum.Type): enum.Type = {
      val enumMap = enum.all.map(v => v.litValue -> v).toMap
      enumMap(Context().backend.peekBits(x))
    }
  }

}
