package lib

import chisel3._


import scala.language.implicitConversions

object util {
  implicit def SeqToVec[T <: Data](x: Seq[T]): Vec[T] = VecInit(x)
  implicit class BundleItemAssignment[T <: Bundle](b: T) {
    def set(assignments: (T => Unit)*): Unit = {
      assignments.foreach(_(b))
    }
  }
  object Delay {
    def apply[T <: Data](x: T, cycles: Int = 1): T = if (cycles == 0) x else apply(RegNext(x, 0.U.asTypeOf(x)), cycles - 1)
  }
  implicit class BoolVec(x: Seq[Bool]) {
    def orR: Bool = x.reduceTree(_ || _)
    def rotatedLeft: Seq[Bool] = x.last +: x.reverse.tail.reverse
  }
  implicit class SeqToVecMethods[T <: Data](x: Seq[T]) {
    def toVec: Vec[T] = VecInit(x)
  }
  implicit class DataReducer[T <: Data](x: Seq[T]) {
    class Pair extends Bundle { val sel = Bool(); val data = chiselTypeOf(x.head) }
    def reduceWithOH(oh: Seq[Bool]): T = {
      x.zip(oh).map { case (data, sel) =>
        val w = Wire(new Pair)
        w.sel := sel
        w.data := data
        w
      }.toVec.reduceTree { (l,r) =>
        Mux(l.sel, l, r)
      }.data
    }
  }
  implicit class ByteSplitter(x: UInt) {
    private val numberOfBytes = scala.math.ceil(x.getWidth / 8.0).toInt
    def toBytes(n: Int = numberOfBytes): Seq[UInt] = {
      val w = WireDefault(Seq.fill(n)(0.U(8.W)).toVec)
      Seq.tabulate(numberOfBytes - 1)(_ * 8).map(i => x(i+7,i)).zip(w).foreach { case (w,v) => w := v }
      w(numberOfBytes - 1) := x(x.getWidth-1, (numberOfBytes - 1) * 8).asTypeOf(UInt(8.W))
      w
    }
  }
  implicit class SeqConcat(x: Seq[UInt]) {
    def concat: UInt = x.reduce(_ ## _)
  }
  implicit class SeqToTransposable[T](x: Seq[Seq[T]]) {
    def T: Seq[Seq[T]] = Seq.tabulate(x.head.length)(i => x.map(_(i)))
  }
  implicit class FieldOptionExtractor[T](top: Option[T]) {
    def getFieldOption[U](field: T => U): Option[U] = {
      if(top.isDefined) Some(field(top.get)) else None
    }
  }
  implicit def ConcreteValueToOption[T](v: T): Option[T] = Some(v)
  implicit def IntToBigInt(x: Int): BigInt = BigInt(x)
  implicit def IntSeqToBigIntSeq(x: Seq[Int]): Seq[BigInt] = x.map(BigInt(_))

  def synchronize[T <: Data](x: T): T = Delay(x, cycles = 2)

  implicit class Flippable[T <: Data](x: T) {
    def flipped: T = Flipped(x)
  }
}

