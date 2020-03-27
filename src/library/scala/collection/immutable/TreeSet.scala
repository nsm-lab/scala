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
package immutable

import scala.collection.Stepper.EfficientSplit
import scala.collection.generic.DefaultSerializable
import scala.collection.immutable.{RedBlackTree => RB}
import scala.collection.mutable.ReusableBuilder


/** This class implements immutable sorted sets using a tree.
  *
  *  @tparam A         the type of the elements contained in this tree set
  *  @param ordering   the implicit ordering used to compare objects of type `A`
  *
  *  @see [[http://docs.scala-lang.org/overviews/collections/concrete-immutable-collection-classes.html#red-black-trees "Scala's Collection Library overview"]]
  *  section on `Red-Black Trees` for more information.
  *
  *  @define Coll `immutable.TreeSet`
  *  @define coll immutable tree set
  *  @define orderDependent
  *  @define orderDependentFold
  *  @define mayNotTerminateInf
  *  @define willNotTerminateInf
  */
final class TreeSet[A] private[immutable] (private[immutable] val tree: RB.Tree[A, Any])(implicit val ordering: Ordering[A])
  extends AbstractSet[A]
    with SortedSet[A]
    with SortedSetOps[A, TreeSet, TreeSet[A]]
    with StrictOptimizedSortedSetOps[A, TreeSet, TreeSet[A]]
    with SortedSetFactoryDefaults[A, TreeSet, Set]
    with DefaultSerializable {

  if (ordering eq null) throw new NullPointerException("ordering must not be null")

  def this()(implicit ordering: Ordering[A]) = this(null)(ordering)

  override def sortedIterableFactory = TreeSet

  private[this] def newSetOrSelf(t: RB.Tree[A, Any]) = if(t eq tree) this else new TreeSet[A](t)

  override def size: Int = RB.count(tree)

  override def isEmpty = size == 0

  override def head: A = RB.smallest(tree).key

  override def last: A = RB.greatest(tree).key

  override def tail: TreeSet[A] = new TreeSet(RB.tail(tree))

  override def init: TreeSet[A] = new TreeSet(RB.init(tree))

  override def min[A1 >: A](implicit ord: Ordering[A1]): A = {
    if ((ord eq ordering) && nonEmpty) {
      head
    } else {
      super.min(ord)
    }
  }

  override def max[A1 >: A](implicit ord: Ordering[A1]): A = {
    if ((ord eq ordering) && nonEmpty) {
      last
    } else {
      super.max(ord)
    }
  }

  override def drop(n: Int): TreeSet[A] = {
    if (n <= 0) this
    else if (n >= size) empty
    else new TreeSet(RB.drop(tree, n))
  }

  override def take(n: Int): TreeSet[A] = {
    if (n <= 0) empty
    else if (n >= size) this
    else new TreeSet(RB.take(tree, n))
  }

  override def slice(from: Int, until: Int): TreeSet[A] = {
    if (until <= from) empty
    else if (from <= 0) take(until)
    else if (until >= size) drop(from)
    else new TreeSet(RB.slice(tree, from, until))
  }

  override def dropRight(n: Int): TreeSet[A] = take(size - math.max(n, 0))

  override def takeRight(n: Int): TreeSet[A] = drop(size - math.max(n, 0))

  private[this] def countWhile(p: A => Boolean): Int = {
    var result = 0
    val it = iterator
    while (it.hasNext && p(it.next())) result += 1
    result
  }
  override def dropWhile(p: A => Boolean): TreeSet[A] = drop(countWhile(p))

  override def takeWhile(p: A => Boolean): TreeSet[A] = take(countWhile(p))

  override def span(p: A => Boolean): (TreeSet[A], TreeSet[A]) = splitAt(countWhile(p))

  override def foreach[U](f: A => U): Unit = RB.foreachKey(tree, f)

  override def minAfter(key: A): Option[A] = {
    val v = RB.minAfter(tree, key)
    if (v eq null) Option.empty else Some(v.key)
  }

  override def maxBefore(key: A): Option[A] = {
    val v = RB.maxBefore(tree, key)
    if (v eq null) Option.empty else Some(v.key)
  }

  def iterator: Iterator[A] = RB.keysIterator(tree)

  def iteratorFrom(start: A): Iterator[A] = RB.keysIterator(tree, Some(start))

  override def stepper[S <: Stepper[_]](implicit shape: StepperShape[A, S]): S with EfficientSplit = {
    import scala.collection.convert.impl._
    type T = RB.Tree[A, Any]
    val s = shape.shape match {
      case StepperShape.IntShape    => IntBinaryTreeStepper.from[T]   (size, tree, _.left, _.right, _.key.asInstanceOf[Int])
      case StepperShape.LongShape   => LongBinaryTreeStepper.from[T]  (size, tree, _.left, _.right, _.key.asInstanceOf[Long])
      case StepperShape.DoubleShape => DoubleBinaryTreeStepper.from[T](size, tree, _.left, _.right, _.key.asInstanceOf[Double])
      case _         => shape.parUnbox(AnyBinaryTreeStepper.from[A, T](size, tree, _.left, _.right, _.key))
    }
    s.asInstanceOf[S with EfficientSplit]
  }

  /** Checks if this set contains element `elem`.
    *
    *  @param  elem    the element to check for membership.
    *  @return true, iff `elem` is contained in this set.
    */
  def contains(elem: A): Boolean = RB.contains(tree, elem)

  override def range(from: A, until: A): TreeSet[A] = newSetOrSelf(RB.range(tree, from, until))

  def rangeImpl(from: Option[A], until: Option[A]): TreeSet[A] = newSetOrSelf(RB.rangeImpl(tree, from, until))

  /** Creates a new `TreeSet` with the entry added.
    *
    *  @param elem    a new element to add.
    *  @return        a new $coll containing `elem` and all the elements of this $coll.
    */
  def incl(elem: A): TreeSet[A] =
    newSetOrSelf(RB.update(tree, elem, null, overwrite = false))

  /** Creates a new `TreeSet` with the entry removed.
    *
    *  @param elem    a new element to add.
    *  @return        a new $coll containing all the elements of this $coll except `elem`.
    */
  def excl(elem: A): TreeSet[A] =
    newSetOrSelf(RB.delete(tree, elem))

  override def concat(that: collection.IterableOnce[A]): TreeSet[A] = {
    val t = that match {
      case ts: TreeSet[A] if ordering == ts.ordering =>
        RB.union(tree, ts.tree)
      case _ =>
        val it = that.iterator
        var t = tree
        while (it.hasNext) t = RB.update(t, it.next(), null, overwrite = false)
        t
    }
    newSetOrSelf(t)
  }

  override def removedAll(that: IterableOnce[A]): TreeSet[A] = that match {
    case ts: TreeSet[A] if ordering == ts.ordering =>
      newSetOrSelf(RB.difference(tree, ts.tree))
    case _ => super.removedAll(that)
  }

  override def intersect(that: collection.Set[A]): TreeSet[A] = that match {
    case ts: TreeSet[A] if ordering == ts.ordering =>
      newSetOrSelf(RB.intersect(tree, ts.tree))
    case _ =>
      super.intersect(that)
  }

  override def diff(that: collection.Set[A]): TreeSet[A] = that match {
    case ts: TreeSet[A] if ordering == ts.ordering =>
      newSetOrSelf(RB.difference(tree, ts.tree))
    case _ =>
      super.diff(that)
  }

  override def filter(f: A => Boolean): TreeSet[A] = newSetOrSelf(RB.filterKeys(tree, f))

  override def partition(p: A => Boolean): (TreeSet[A], TreeSet[A]) = {
    val (l, r) = RB.partitionKeys(tree, p)
    (newSetOrSelf(l), newSetOrSelf(r))
  }

  override protected[this] def className = "TreeSet"
}

/**
  * $factoryInfo
  *
  *  @define Coll `immutable.TreeSet`
  *  @define coll immutable tree set
  */
@SerialVersionUID(3L)
object TreeSet extends SortedIterableFactory[TreeSet] {

  def empty[A: Ordering]: TreeSet[A] = new TreeSet[A]

  def from[E](it: scala.collection.IterableOnce[E])(implicit ordering: Ordering[E]): TreeSet[E] =
    it match {
      case ts: TreeSet[E] if ordering == ts.ordering => ts
      case ss: scala.collection.SortedSet[E] if ordering == ss.ordering =>
        new TreeSet[E](RB.fromOrderedKeys(ss.iterator, ss.size))
      case r: Range if (ordering eq Ordering.Int) || (Ordering.Int isReverseOf ordering) =>
        val it = if((ordering eq Ordering.Int) == (r.step > 0)) r.iterator else r.reverseIterator
        val tree = RB.fromOrderedKeys(it.asInstanceOf[Iterator[E]], r.size)
          // The cast is needed to compile with Dotty:
          // Dotty doesn't infer that E =:= Int, since instantiation of covariant GADTs is unsound
        new TreeSet[E](tree)
      case _ =>
        var t: RB.Tree[E, Null] = null
        val i = it.iterator
        while (i.hasNext) t = RB.update(t, i.next(), null, overwrite = false)
        new TreeSet[E](t)
    }

  def newBuilder[A](implicit ordering: Ordering[A]): ReusableBuilder[A, TreeSet[A]] = new ReusableBuilder[A, TreeSet[A]] {
    private[this] var tree: RB.Tree[A, Any] = null
    def addOne(elem: A): this.type = { tree = RB.update(tree, elem, null, overwrite = false); this }
    override def addAll(xs: IterableOnce[A]): this.type = {
      xs match {
        case ts: TreeSet[A] if ordering == ts.ordering =>
          tree = RB.union(tree, ts.tree)
        case _ =>
          val it = xs.iterator
          while (it.hasNext) tree = RB.update(tree, it.next(), null, overwrite = false)
      }
      this
    }
    def result(): TreeSet[A] = if(tree eq null) TreeSet.empty else new TreeSet[A](tree)
    def clear(): Unit = { tree = null }
  }
}
