/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package scala
package collection


/** View defined in terms of indexing a range */
trait IndexedSeqView[+A] extends IndexedSeqOps[A, View, View[A]] with SeqView[A] { self =>

  override def view: IndexedSeqView[A] = this

  @deprecated("Use .view.slice(from, until) instead of .view(from, until)", "2.13.0")
  override def view(from: Int, until: Int): IndexedSeqView[A] = view.slice(from, until)

  override def iterator: Iterator[A] = new IndexedSeqView.IndexedSeqViewIterator(this)
  override def reverseIterator: Iterator[A] = new IndexedSeqView.IndexedSeqViewReverseIterator(this)

  override def appended[B >: A](elem: B): IndexedSeqView[B] = new IndexedSeqView.Appended(this, elem)
  override def prepended[B >: A](elem: B): IndexedSeqView[B] = new IndexedSeqView.Prepended(elem, this)
  override def take(n: Int): IndexedSeqView[A] = new IndexedSeqView.Take(this, n)
  override def takeRight(n: Int): IndexedSeqView[A] = new IndexedSeqView.TakeRight(this, n)
  override def drop(n: Int): IndexedSeqView[A] = new IndexedSeqView.Drop(this, n)
  override def dropRight(n: Int): IndexedSeqView[A] = new IndexedSeqView.DropRight(this, n)
  override def map[B](f: A => B): IndexedSeqView[B] = new IndexedSeqView.Map(this, f)
  override def reverse: IndexedSeqView[A] = new IndexedSeqView.Reverse(this)
  override def slice(from: Int, until: Int): IndexedSeqView[A] = new IndexedSeqView.Slice(this, from, until)
  override def tapEach[U](f: A => U): IndexedSeqView[A] = new IndexedSeqView.Map(this, { (a: A) => f(a); a})

  def concat[B >: A](suffix: IndexedSeqView.SomeIndexedSeqOps[B]): IndexedSeqView[B] = new IndexedSeqView.Concat(this, suffix)
  def appendedAll[B >: A](suffix: IndexedSeqView.SomeIndexedSeqOps[B]): IndexedSeqView[B] = new IndexedSeqView.Concat(this, suffix)
  def prependedAll[B >: A](prefix: IndexedSeqView.SomeIndexedSeqOps[B]): IndexedSeqView[B] = new IndexedSeqView.Concat(prefix, this)

  override protected[this] def stringPrefix: String = "IndexedSeqView"
}

object IndexedSeqView {

  @SerialVersionUID(3L)
  private final class IndexedSeqViewIterator[A](self: IndexedSeqView[A]) extends AbstractIterator[A] with Serializable {
    private[this] var current = 0
    private[this] var remainder = self.size
    override def knownSize: Int = remainder
    def hasNext = remainder > 0
    def next(): A =
      if (hasNext) {
        val r = self.apply(current)
        current += 1
        remainder -= 1
        r
      } else Iterator.empty.next()

    override def drop(n: Int): Iterator[A] = {
      if (n > 0) {
        current += n
        remainder = Math.max(0, remainder - n)
      }
      this
    }

    override protected def sliceIterator(from: Int, until: Int): Iterator[A] = {

      def formatRange(value : Int) : Int = if (value < 0) 0 else if (value > remainder) remainder else value

      val formatFrom = formatRange(from)
      val formatUntil = formatRange(until)
      remainder = Math.max(0, formatUntil - formatFrom)
      current = current + formatFrom
      this
    }
  }
  @SerialVersionUID(3L)
  private final class IndexedSeqViewReverseIterator[A](self: IndexedSeqView[A]) extends AbstractIterator[A] with Serializable {
    private[this] var pos = self.size - 1
    private[this] var remainder = self.size
    def hasNext: Boolean = remainder > 0
    def next(): A =
      if (pos < 0) throw new NoSuchElementException
      else {
        val r = self(pos)
        pos -= 1
        remainder -= 1
        r
      }

    override def drop(n: Int): Iterator[A] = {
      if (n > 0) {
        pos -= n
        remainder = Math.max(0, remainder - n)
      }
      this
    }


    override def sliceIterator(from: Int, until: Int): Iterator[A] = {
      val startCutoff = pos
      val untilCutoff = startCutoff - remainder + 1
      val nextStartCutoff = if (from < 0) startCutoff else if (startCutoff - from < 0) 0 else startCutoff - from
      val nextUntilCutoff = if (until < 0) startCutoff else if (startCutoff - until < untilCutoff) untilCutoff else startCutoff - until + 1
      remainder = Math.max(0, nextStartCutoff - nextUntilCutoff + 1)
      pos = nextStartCutoff
      this
    }
  }

  /** An `IndexedSeqOps` whose collection type and collection type constructor are unknown */
  type SomeIndexedSeqOps[A] = IndexedSeqOps[A, AnyConstr, _]

  @SerialVersionUID(3L)
  class Id[+A](underlying: SomeIndexedSeqOps[A])
    extends SeqView.Id(underlying) with IndexedSeqView[A]

  @SerialVersionUID(3L)
  class Appended[+A](underlying: SomeIndexedSeqOps[A], elem: A)
    extends SeqView.Appended(underlying, elem) with IndexedSeqView[A]

  @SerialVersionUID(3L)
  class Prepended[+A](elem: A, underlying: SomeIndexedSeqOps[A])
    extends SeqView.Prepended(elem, underlying) with IndexedSeqView[A]

  @SerialVersionUID(3L)
  class Concat[A](prefix: SomeIndexedSeqOps[A], suffix: SomeIndexedSeqOps[A])
    extends SeqView.Concat[A](prefix, suffix) with IndexedSeqView[A]

  @SerialVersionUID(3L)
  class Take[A](underlying: SomeIndexedSeqOps[A], n: Int)
    extends SeqView.Take(underlying, n) with IndexedSeqView[A]

  @SerialVersionUID(3L)
  class TakeRight[A](underlying: SomeIndexedSeqOps[A], n: Int)
    extends SeqView.TakeRight(underlying, n) with IndexedSeqView[A]

  @SerialVersionUID(3L)
  class Drop[A](underlying: SomeIndexedSeqOps[A], n: Int)
    extends SeqView.Drop[A](underlying, n) with IndexedSeqView[A]

  @SerialVersionUID(3L)
  class DropRight[A](underlying: SomeIndexedSeqOps[A], n: Int)
    extends SeqView.DropRight[A](underlying, n) with IndexedSeqView[A]

  @SerialVersionUID(3L)
  class Map[A, B](underlying: SomeIndexedSeqOps[A], f: A => B)
    extends SeqView.Map(underlying, f) with IndexedSeqView[B]

  @SerialVersionUID(3L)
  class Reverse[A](underlying: SomeIndexedSeqOps[A]) extends SeqView.Reverse[A](underlying) with IndexedSeqView[A] {
    override def reverse: IndexedSeqView[A] = underlying match {
      case x: IndexedSeqView[A] => x
      case _ => super.reverse
    }
  }

  @SerialVersionUID(3L)
  class Slice[A](underlying: SomeIndexedSeqOps[A], from: Int, until: Int) extends AbstractIndexedSeqView[A] {
    protected val lo = from max 0
    protected val hi = (until max 0) min underlying.length
    protected val len = (hi - lo) max 0
    @throws[IndexOutOfBoundsException]
    def apply(i: Int): A = underlying(lo + i)
    def length: Int = len
  }
}

/** Explicit instantiation of the `IndexedSeqView` trait to reduce class file size in subclasses. */
@SerialVersionUID(3L)
abstract class AbstractIndexedSeqView[+A] extends AbstractSeqView[A] with IndexedSeqView[A]
