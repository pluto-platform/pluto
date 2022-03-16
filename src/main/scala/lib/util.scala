package lib

import chisel3._
import lib.util.SeqToTransposable

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
  }
  implicit class SeqToVecMethods[T <: Data](x: Seq[T]) {
    def toVec: Vec[T] = VecInit(x)
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
}
