package plutocore.lib

import chisel3.util.Fill
import chisel3._

object Immediates {

  implicit class FromInstructionToImmediate(i: UInt) {
    require(i.getWidth == 32)

    object extractImmediate {
      def iType: SInt = (Fill(21, i(31)) ## i(30, 25) ## i(24, 21) ## i(20)).asSInt

      def sType: SInt = (Fill(21, i(31)) ## i(30, 25) ## i(11, 8) ## i(7)).asSInt

      def bType: SInt = (Fill(20, i(31)) ## i(7) ## i(30, 25) ## i(11, 8) ## 0.B).asSInt

      def uType: SInt = (i(31, 12) ## Fill(12, 0.B)).asSInt

      def jType: SInt = (Fill(10, i(31)) ## i(19, 12) ## i(20) ## i(30, 25) ## i(24, 21) ## 0.B).asSInt
    }

  }

}
