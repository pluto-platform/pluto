package lib

import chisel3.Bundle

object util {
  implicit class BundleItemAssignment[T <: Bundle](b: T) {
    def set(assignments: (T => Unit)*): Unit = {
      assignments.foreach(_(b))
    }
  }
}
