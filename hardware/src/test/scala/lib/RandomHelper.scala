package lib

import chisel3._
import chisel3.internal.firrtl.Width

object RandomHelper {

  def uRand(max: Int): UInt = scala.util.Random.nextInt(max).U
  def uRands(max: Int, maxs: Int*): Seq[UInt] = uRand(max) +: maxs.map(uRand)
  def uRand(range: Range): UInt = (range.min + scala.util.Random.nextInt(range.max - range.min)).U
  def uRands(range: Range, ranges: Range*): Seq[UInt] = uRand(range) +: ranges.map(uRand)
  def uRand(width: Width): UInt = scala.util.Random.nextInt(scala.math.pow(2,width.get).toInt).U
  def uRands(width: Width, widths: Width*): Seq[UInt] = uRand(width) +: widths.map(uRand)

}