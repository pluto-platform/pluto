package lib

import chisel3._

object Types {

  object Byte { def apply(): UInt = UInt(8.W) }
  object HalfWord { def apply(): UInt = UInt(16.W) }
  object Word { def apply(): UInt = UInt(32.W) }

}
