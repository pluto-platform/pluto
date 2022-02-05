package lib

import chisel3.Data
import chisel3.util.MuxLookup

object LookUp {

  case class lookUp[S <: Data](key: S) {
    def in[T <: Data](pairs: (S, T)*): SelectBuilder[S, T] = SelectBuilder(key, pairs: _*)
  }

  case class SelectBuilder[S <: Data, T <: Data](sel: S, pairs: (S, T)*) {
    def orElse(default: T): T = {
      MuxLookup(sel.asUInt, default, pairs.map { case (s, t) => (s.asUInt, t) })
    }
  }
}