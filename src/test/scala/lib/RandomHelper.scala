package lib

import chisel3._
import chisel3.internal.firrtl.Width

object RandomHelper {

  def uRand(max: Long): UInt = (scala.util.Random.nextDouble() * max).toLong.U
  def uRands(max: Int, maxs: Int*): Seq[UInt] = uRand(max) +: maxs.map(uRand(_))
  def uRand(range: Range): UInt = (range.min + (scala.util.Random.nextDouble() * (range.max - range.min)).toLong).U
  def uRands(range: Range, ranges: Range*): Seq[UInt] = uRand(range) +: ranges.map(uRand)
  def uRand(width: Width): UInt = BigInt(width.get, scala.util.Random).U
  def uRands(width: Width, widths: Width*): Seq[UInt] = uRand(width) +: widths.map(uRand)

}