package cache

import chisel3._
import chisel3.util.{isPow2, log2Ceil}

import scala.collection.immutable.NumericRange

object Cache {

  case class Dimension(
                        size: Int, // total size in bytes
                        blockSize: Int, // size of one block in bytes
                        coverage: NumericRange.Exclusive[BigInt], // total number of bytes covered by the cache
                      ) {
    require((coverage.start % 4) == 0 && (coverage.end % 4) == 0, s"Cache coverage range must be byte aligned")
    require(isPow2(size),s"Cache size needs to be a power of two but was $size")
    require(isPow2(blockSize),s"Cache block size needs to a power of two but was $blockSize")

    val lines = size / blockSize
    val wordsPerLine = blockSize / 4

    object Widths {
      val address = log2Ceil(coverage.length)
      val byteOffest = 2
      val blockOffset = log2Ceil(wordsPerLine)
      val index = log2Ceil(size / blockSize)
      val tag = 32 - index - blockOffset - byteOffest
    }
  }

  object LineInfo {
    def apply()(implicit dim: Cache.Dimension): LineInfo = new LineInfo
    def apply(valid: Bool, tag: UInt)(implicit dim: Cache.Dimension): LineInfo = {
      val l = Wire(LineInfo())
      l.valid := valid
      l.tag := tag
      l
    }
  }
  class LineInfo(implicit dim: Cache.Dimension) extends Bundle {
    val valid = Bool()
    val tag = UInt(dim.Widths.tag.W)
  }

  implicit class AddressSplitter(x: UInt)(implicit dim: Cache.Dimension) {
    def getByteOffset: UInt = x(1,0)
    def getBlockOffset: UInt = x(dim.Widths.blockOffset + 1, 2)
    def getIndex: UInt = x(dim.Widths.index + dim.Widths.blockOffset + 1, dim.Widths.blockOffset + 2)
    def getTag: UInt = x(dim.Widths.address - 1, dim.Widths.index + dim.Widths.blockOffset + 2)
  }

}
