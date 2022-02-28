package lib

import chisel3.Data
import chisel3.util.MuxLookup

object LookUp {

  case class lookUp[S <: Data](key: S) {
    def in[T <: Data](pairs: (S, T)*): T = {
      MuxLookup(key.asUInt, pairs.head._2, pairs.map { case (s, t) => (s.asUInt, t) })
    }
    def in[T <: Data](default: T, pairs: (S, T)*): T = {
      MuxLookup(key.asUInt, default, pairs.map { case (s, t) => (s.asUInt, t) })
    }
  }
}