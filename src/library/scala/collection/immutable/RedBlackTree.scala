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

import collection.Iterator

import scala.annotation.tailrec
import scala.annotation.meta.getter

import java.lang.{Integer, String}

/** An object containing the RedBlack tree implementation used by for `TreeMaps` and `TreeSets`.
  *
  *  Implementation note: since efficiency is important for data structures this implementation
  *  uses `null` to represent empty trees. This also means pattern matching cannot
  *  easily be used. The API represented by the RedBlackTree object tries to hide these
  *  optimizations behind a reasonably clean API.
  */
private[collection] object RedBlackTree {

  def isEmpty(tree: Tree[_, _]): Boolean = tree eq null

  def contains[A: Ordering](tree: Tree[A, _], x: A): Boolean = lookup(tree, x) ne null
  def get[A: Ordering, B](tree: Tree[A, B], x: A): Option[B] = lookup(tree, x) match {
    case null => None
    case tree => Some(tree.value)
  }

  @tailrec
  def lookup[A, B](tree: Tree[A, B], x: A)(implicit ordering: Ordering[A]): Tree[A, B] = if (tree eq null) null else {
    val cmp = ordering.compare(x, tree.key)
    if (cmp < 0) lookup(tree.left, x)
    else if (cmp > 0) lookup(tree.right, x)
    else tree
  }

  def count(tree: Tree[_, _]) = if (tree eq null) 0 else tree.count
  def update[A: Ordering, B, B1 >: B](tree: Tree[A, B], k: A, v: B1, overwrite: Boolean): Tree[A, B1] = blacken(upd(tree, k, v, overwrite))
  def delete[A: Ordering, B](tree: Tree[A, B], k: A): Tree[A, B] = blacken(del(tree, k))
  def rangeImpl[A: Ordering, B](tree: Tree[A, B], from: Option[A], until: Option[A]): Tree[A, B] = (from, until) match {
    case (Some(from), Some(until)) => this.range(tree, from, until)
    case (Some(from), None)        => this.from(tree, from)
    case (None,       Some(until)) => this.until(tree, until)
    case (None,       None)        => tree
  }
  def range[A: Ordering, B](tree: Tree[A, B], from: A, until: A): Tree[A, B] = blacken(doRange(tree, from, until))
  def from[A: Ordering, B](tree: Tree[A, B], from: A): Tree[A, B] = blacken(doFrom(tree, from))
  def to[A: Ordering, B](tree: Tree[A, B], to: A): Tree[A, B] = blacken(doTo(tree, to))
  def until[A: Ordering, B](tree: Tree[A, B], key: A): Tree[A, B] = blacken(doUntil(tree, key))

  def drop[A: Ordering, B](tree: Tree[A, B], n: Int): Tree[A, B] = blacken(doDrop(tree, n))
  def take[A: Ordering, B](tree: Tree[A, B], n: Int): Tree[A, B] = blacken(doTake(tree, n))
  def slice[A: Ordering, B](tree: Tree[A, B], from: Int, until: Int): Tree[A, B] = blacken(doSlice(tree, from, until))

  def smallest[A, B](tree: Tree[A, B]): Tree[A, B] = {
    if (tree eq null) throw new NoSuchElementException("empty tree")
    var result = tree
    while (result.left ne null) result = result.left
    result
  }
  def greatest[A, B](tree: Tree[A, B]): Tree[A, B] = {
    if (tree eq null) throw new NoSuchElementException("empty tree")
    var result = tree
    while (result.right ne null) result = result.right
    result
  }

  def tail[A, B](tree: Tree[A, B]): Tree[A, B] = {
    def _tail(tree: Tree[A, B]): Tree[A, B] =
      if(tree eq null) throw new NoSuchElementException("empty tree")
      else if(tree.left eq null) tree.right
      else if(isBlackTree(tree.left)) balLeft(tree.key, tree.value, _tail(tree.left), tree.right)
      else RedTree(tree.key, tree.value, _tail(tree.left), tree.right)
    blacken(_tail(tree))
  }

  def init[A, B](tree: Tree[A, B]): Tree[A, B] = {
    def _init(tree: Tree[A, B]): Tree[A, B] =
      if(tree eq null) throw new NoSuchElementException("empty tree")
      else if(tree.right eq null) tree.left
      else if(isBlackTree(tree.right)) balRight(tree.key, tree.value, tree.left, _init(tree.right))
      else RedTree(tree.key, tree.value, tree.left, _init(tree.right))
    blacken(_init(tree))
  }

  /**
    * Returns the smallest node with a key larger than or equal to `x`. Returns `null` if there is no such node.
    */
  def minAfter[A, B](tree: Tree[A, B], x: A)(implicit ordering: Ordering[A]): Tree[A, B] = if (tree eq null) null else {
    val cmp = ordering.compare(x, tree.key)
    if (cmp == 0) tree
    else if (cmp < 0) {
      val l = minAfter(tree.left, x)
      if (l != null) l else tree
    } else minAfter(tree.right, x)
  }

  /**
    * Returns the largest node with a key smaller than `x`. Returns `null` if there is no such node.
    */
  def maxBefore[A, B](tree: Tree[A, B], x: A)(implicit ordering: Ordering[A]): Tree[A, B] = if (tree eq null) null else {
    val cmp = ordering.compare(x, tree.key)
    if (cmp <= 0) maxBefore(tree.left, x)
    else {
      val r = maxBefore(tree.right, x)
      if (r != null) r else tree
    }
  }

  def foreach[A,B,U](tree:Tree[A,B], f:((A,B)) => U):Unit = if (tree ne null) _foreach(tree,f)

  private[this] def _foreach[A, B, U](tree: Tree[A, B], f: ((A, B)) => U): Unit = {
    if (tree.left ne null) _foreach(tree.left, f)
    f((tree.key, tree.value))
    if (tree.right ne null) _foreach(tree.right, f)
  }

  def foreachKey[A, U](tree:Tree[A,_], f: A => U):Unit = if (tree ne null) _foreachKey(tree,f)

  private[this] def _foreachKey[A, U](tree: Tree[A, _], f: A => U): Unit = {
    if (tree.left ne null) _foreachKey(tree.left, f)
    f((tree.key))
    if (tree.right ne null) _foreachKey(tree.right, f)
  }

  def foreachEntry[A, B, U](tree:Tree[A,B], f: (A, B) => U):Unit = if (tree ne null) _foreachEntry(tree,f)

  private[this] def _foreachEntry[A, B, U](tree: Tree[A, B], f: (A, B) => U): Unit = {
    if (tree.left ne null) _foreachEntry(tree.left, f)
    f(tree.key, tree.value)
    if (tree.right ne null) _foreachEntry(tree.right, f)
  }

  def iterator[A: Ordering, B](tree: Tree[A, B], start: Option[A] = None): Iterator[(A, B)] = new EntriesIterator(tree, start)
  def keysIterator[A: Ordering](tree: Tree[A, _], start: Option[A] = None): Iterator[A] = new KeysIterator(tree, start)
  def valuesIterator[A: Ordering, B](tree: Tree[A, B], start: Option[A] = None): Iterator[B] = new ValuesIterator(tree, start)

  @tailrec
  def nth[A, B](tree: Tree[A, B], n: Int): Tree[A, B] = {
    val count = this.count(tree.left)
    if (n < count) nth(tree.left, n)
    else if (n > count) nth(tree.right, n - count - 1)
    else tree
  }

  def isBlack(tree: Tree[_, _]) = (tree eq null) || isBlackTree(tree)

  @`inline` private[this] def isRedTree(tree: Tree[_, _]) = tree.isInstanceOf[RedTree[_, _]]
  @`inline` private[this] def isBlackTree(tree: Tree[_, _]) = tree.isInstanceOf[BlackTree[_, _]]

  private[this] def blacken[A, B](t: Tree[A, B]): Tree[A, B] = if (t eq null) null else t.black

  // Blacken if the tree is red and has a red child. This is necessary when using methods such as `upd` or `updNth`
  // for building subtrees. Use `blacken` instead when building top-level trees.
  private[this] def maybeBlacken[A, B](t: Tree[A, B]): Tree[A, B] =
    if(isBlack(t)) t else if(isRedTree(t.left) || isRedTree(t.right)) t.black else t

  private[this] def mkTree[A, B](isBlack: Boolean, k: A, v: B, l: Tree[A, B], r: Tree[A, B]) =
    if (isBlack) BlackTree(k, v, l, r) else RedTree(k, v, l, r)

  private[this] def balanceLeft[A, B, B1 >: B](isBlack: Boolean, z: A, zv: B, l: Tree[A, B1], d: Tree[A, B1]): Tree[A, B1] = {
    if (isRedTree(l) && isRedTree(l.left))
      RedTree(l.key, l.value, BlackTree(l.left.key, l.left.value, l.left.left, l.left.right), BlackTree(z, zv, l.right, d))
    else if (isRedTree(l) && isRedTree(l.right))
      RedTree(l.right.key, l.right.value, BlackTree(l.key, l.value, l.left, l.right.left), BlackTree(z, zv, l.right.right, d))
    else
      mkTree(isBlack, z, zv, l, d)
  }
  private[this] def balanceRight[A, B, B1 >: B](isBlack: Boolean, x: A, xv: B, a: Tree[A, B1], r: Tree[A, B1]): Tree[A, B1] = {
    if (isRedTree(r) && isRedTree(r.left))
      RedTree(r.left.key, r.left.value, BlackTree(x, xv, a, r.left.left), BlackTree(r.key, r.value, r.left.right, r.right))
    else if (isRedTree(r) && isRedTree(r.right))
      RedTree(r.key, r.value, BlackTree(x, xv, a, r.left), BlackTree(r.right.key, r.right.value, r.right.left, r.right.right))
    else
      mkTree(isBlack, x, xv, a, r)
  }
  private[this] def upd[A, B, B1 >: B](tree: Tree[A, B], k: A, v: B1, overwrite: Boolean)(implicit ordering: Ordering[A]): Tree[A, B1] = if (tree eq null) {
    RedTree(k, v, null, null)
  } else {
    val cmp = ordering.compare(k, tree.key)
    if (cmp < 0) balanceLeft(isBlackTree(tree), tree.key, tree.value, upd(tree.left, k, v, overwrite), tree.right)
    else if (cmp > 0) balanceRight(isBlackTree(tree), tree.key, tree.value, tree.left, upd(tree.right, k, v, overwrite))
    else if (overwrite || k != tree.key) mkTree(isBlackTree(tree), tree.key, v, tree.left, tree.right)
    else tree
  }
  private[this] def updNth[A, B, B1 >: B](tree: Tree[A, B], idx: Int, k: A, v: B1, overwrite: Boolean): Tree[A, B1] = if (tree eq null) {
    RedTree(k, v, null, null)
  } else {
    val rank = count(tree.left) + 1
    if (idx < rank) balanceLeft(isBlackTree(tree), tree.key, tree.value, updNth(tree.left, idx, k, v, overwrite), tree.right)
    else if (idx > rank) balanceRight(isBlackTree(tree), tree.key, tree.value, tree.left, updNth(tree.right, idx - rank, k, v, overwrite))
    else if (overwrite) mkTree(isBlackTree(tree), k, v, tree.left, tree.right)
    else tree
  }

  private[this] def doFrom[A, B](tree: Tree[A, B], from: A)(implicit ordering: Ordering[A]): Tree[A, B] = {
    if (tree eq null) return null
    if (ordering.lt(tree.key, from)) return doFrom(tree.right, from)
    val newLeft = doFrom(tree.left, from)
    if (newLeft eq tree.left) tree
    else if (newLeft eq null) upd(tree.right, tree.key, tree.value, overwrite = false)
    else join(newLeft, tree.key, tree.value, tree.right)
  }
  private[this] def doTo[A, B](tree: Tree[A, B], to: A)(implicit ordering: Ordering[A]): Tree[A, B] = {
    if (tree eq null) return null
    if (ordering.lt(to, tree.key)) return doTo(tree.left, to)
    val newRight = doTo(tree.right, to)
    if (newRight eq tree.right) tree
    else if (newRight eq null) upd(tree.left, tree.key, tree.value, overwrite = false)
    else join (tree.left, tree.key, tree.value, newRight)
  }
  private[this] def doUntil[A, B](tree: Tree[A, B], until: A)(implicit ordering: Ordering[A]): Tree[A, B] = {
    if (tree eq null) return null
    if (ordering.lteq(until, tree.key)) return doUntil(tree.left, until)
    val newRight = doUntil(tree.right, until)
    if (newRight eq tree.right) tree
    else if (newRight eq null) upd(tree.left, tree.key, tree.value, overwrite = false)
    else join(tree.left, tree.key, tree.value, newRight)
  }

  private[this] def doRange[A, B](tree: Tree[A, B], from: A, until: A)(implicit ordering: Ordering[A]): Tree[A, B] = {
    if (tree eq null) return null
    if (ordering.lt(tree.key, from)) return doRange(tree.right, from, until)
    if (ordering.lteq(until, tree.key)) return doRange(tree.left, from, until)
    val newLeft = doFrom(tree.left, from)
    val newRight = doUntil(tree.right, until)
    if ((newLeft eq tree.left) && (newRight eq tree.right)) tree
    else if (newLeft eq null) upd(newRight, tree.key, tree.value, overwrite = false)
    else if (newRight eq null) upd(newLeft, tree.key, tree.value, overwrite = false)
    else join(newLeft, tree.key, tree.value, newRight)
  }

  private[this] def doDrop[A, B](tree: Tree[A, B], n: Int): Tree[A, B] =
    if((tree eq null) || (n <= 0)) tree
    else if(n >= tree.count) null
    else {
      val l = count(tree.left)
      if(n > l) doDrop(tree.right, n-l-1)
      else if(n == l) join(null, tree.key, tree.value, tree.right)
      else join(doDrop(tree.left, n), tree.key, tree.value, tree.right)
    }

  private[this] def doTake[A, B](tree: Tree[A, B], n: Int): Tree[A, B] =
    if((tree eq null) || (n <= 0)) null
    else if(n >= tree.count) tree
    else {
      val l = count(tree.left)
      if(n <= l) doTake(tree.left, n)
      else if(n == l+1) maybeBlacken(updNth(tree.left, n, tree.key, tree.value, overwrite = false))
      else join(tree.left, tree.key, tree.value, doTake(tree.right, n-l-1))
    }

  private[this] def doSlice[A, B](tree: Tree[A, B], from: Int, until: Int): Tree[A, B] =
    if((tree eq null) || (from >= until) || (from >= tree.count) || (until <= 0)) null
    else if((from <= 0) && (until >= tree.count)) tree
    else {
      val l = count(tree.left)
      if(until <= l) doSlice(tree.left, from, until)
      else if(from > l) doSlice(tree.right, from-l-1, until-l-1)
      else join(doDrop(tree.left, from), tree.key, tree.value, doTake(tree.right, until-l-1))
    }

  /*
   * Forcing direct fields access using the @`inline` annotation helps speed up
   * various operations (especially smallest/greatest and update/delete).
   *
   * Unfortunately the direct field access is not guaranteed to work (but
   * works on the current implementation of the Scala compiler).
   *
   * An alternative is to implement the these classes using plain old Java code...
   */
  sealed abstract class Tree[A, +B](
    @(`inline` @getter) final val key: A,
    @(`inline` @getter) final val value: B,
    @(`inline` @getter) final val left: Tree[A, B],
    @(`inline` @getter) final val right: Tree[A, B])
  {
    @(`inline` @getter) final val count: Int = 1 + RedBlackTree.count(left) + RedBlackTree.count(right)
    def black: Tree[A, B]
    def red: Tree[A, B]
  }
  final class RedTree[A, +B](key: A,
    value: B,
    left: Tree[A, B],
    right: Tree[A, B]) extends Tree[A, B](key, value, left, right) {
    override def black: Tree[A, B] = BlackTree(key, value, left, right)
    override def red: Tree[A, B] = this
    override def toString: String = "RedTree(" + key + ", " + value + ", " + left + ", " + right + ")"
  }
  final class BlackTree[A, +B](key: A,
    value: B,
    left: Tree[A, B],
    right: Tree[A, B]) extends Tree[A, B](key, value, left, right) {
    override def black: Tree[A, B] = this
    override def red: Tree[A, B] = RedTree(key, value, left, right)
    override def toString: String = "BlackTree(" + key + ", " + value + ", " + left + ", " + right + ")"
  }

  object RedTree {
    @`inline` def apply[A, B](key: A, value: B, left: Tree[A, B], right: Tree[A, B]) = new RedTree(key, value, left, right)
    def unapply[A, B](t: RedTree[A, B]) = Some((t.key, t.value, t.left, t.right))
  }
  object BlackTree {
    @`inline` def apply[A, B](key: A, value: B, left: Tree[A, B], right: Tree[A, B]) = new BlackTree(key, value, left, right)
    def unapply[A, B](t: BlackTree[A, B]) = Some((t.key, t.value, t.left, t.right))
  }

  private[this] abstract class TreeIterator[A, B, R](root: Tree[A, B], start: Option[A])(implicit ordering: Ordering[A]) extends AbstractIterator[R] {
    protected[this] def nextResult(tree: Tree[A, B]): R

    override def hasNext: Boolean = lookahead ne null

    @throws[NoSuchElementException]
    override def next(): R = {
      val tree = lookahead
      if(tree ne null) {
        lookahead = findLeftMostOrPopOnEmpty(goRight(tree))
        nextResult(tree)
      } else Iterator.empty.next()
    }

    @tailrec
    private[this] def findLeftMostOrPopOnEmpty(tree: Tree[A, B]): Tree[A, B] =
      if (tree eq null) popNext()
      else if (tree.left eq null) tree
      else findLeftMostOrPopOnEmpty(goLeft(tree))

    private[this] def pushNext(tree: Tree[A, B]): Unit = {
      stackOfNexts(index) = tree
      index += 1
    }
    private[this] def popNext(): Tree[A, B] = if (index == 0) null else {
      index -= 1
      stackOfNexts(index)
    }

    private[this] var stackOfNexts = if (root eq null) null else {
      /*
       * According to "Ralf Hinze. Constructing red-black trees" [http://www.cs.ox.ac.uk/ralf.hinze/publications/#P5]
       * the maximum height of a red-black tree is 2*log_2(n + 2) - 2.
       *
       * According to {@see Integer#numberOfLeadingZeros} ceil(log_2(n)) = (32 - Integer.numberOfLeadingZeros(n - 1))
       *
       * Although we don't store the deepest nodes in the path during iteration,
       * we potentially do so in `startFrom`.
       */
      val maximumHeight = 2 * (32 - Integer.numberOfLeadingZeros(root.count + 2 - 1)) - 2
      new Array[Tree[A, B]](maximumHeight)
    }
    private[this] var index = 0
    private[this] var lookahead: Tree[A, B] = start map startFrom getOrElse findLeftMostOrPopOnEmpty(root)

    /**
      * Find the leftmost subtree whose key is equal to the given key, or if no such thing,
      * the leftmost subtree with the key that would be "next" after it according
      * to the ordering. Along the way build up the iterator's path stack so that "next"
      * functionality works.
      */
    private[this] def startFrom(key: A) : Tree[A,B] = if (root eq null) null else {
      @tailrec def find(tree: Tree[A, B]): Tree[A, B] =
        if (tree eq null) popNext()
        else find(
          if (ordering.lteq(key, tree.key)) goLeft(tree)
          else goRight(tree)
        )
      find(root)
    }

    @`inline` private[this] def goLeft(tree: Tree[A, B]) = {
      pushNext(tree)
      tree.left
    }

    @`inline` private[this] def goRight(tree: Tree[A, B]) = tree.right
  }

  private[this] class EntriesIterator[A: Ordering, B](tree: Tree[A, B], focus: Option[A]) extends TreeIterator[A, B, (A, B)](tree, focus) {
    override def nextResult(tree: Tree[A, B]) = (tree.key, tree.value)
  }

  private[this] class KeysIterator[A: Ordering, B](tree: Tree[A, B], focus: Option[A]) extends TreeIterator[A, B, A](tree, focus) {
    override def nextResult(tree: Tree[A, B]) = tree.key
  }

  private[this] class ValuesIterator[A: Ordering, B](tree: Tree[A, B], focus: Option[A]) extends TreeIterator[A, B, B](tree, focus) {
    override def nextResult(tree: Tree[A, B]) = tree.value
  }

  /** Build a Tree suitable for a TreeSet from an ordered sequence of keys */
  def fromOrderedKeys[A](xs: Iterator[A], size: Int): Tree[A, Null] = {
    val maxUsedDepth = 32 - Integer.numberOfLeadingZeros(size) // maximum depth of non-leaf nodes
    def f(level: Int, size: Int): Tree[A, Null] = size match {
      case 0 => null
      case 1 => mkTree(level != maxUsedDepth || level == 1, xs.next(), null, null, null)
      case n =>
        val leftSize = (size-1)/2
        val left = f(level+1, leftSize)
        val x = xs.next()
        val right = f(level+1, size-1-leftSize)
        BlackTree(x, null, left, right)
    }
    f(1, size)
  }

  /** Build a Tree suitable for a TreeMap from an ordered sequence of key/value pairs */
  def fromOrderedEntries[A, B](xs: Iterator[(A, B)], size: Int): Tree[A, B] = {
    val maxUsedDepth = 32 - Integer.numberOfLeadingZeros(size) // maximum depth of non-leaf nodes
    def f(level: Int, size: Int): Tree[A, B] = size match {
      case 0 => null
      case 1 =>
        val (k, v) = xs.next()
        mkTree(level != maxUsedDepth || level == 1, k, v, null, null)
      case n =>
        val leftSize = (size-1)/2
        val left = f(level+1, leftSize)
        val (k, v) = xs.next()
        val right = f(level+1, size-1-leftSize)
        BlackTree(k, v, left, right)
    }
    f(1, size)
  }

  def transform[A, B, C](t: Tree[A, B], f: (A, B) => C): Tree[A, C] =
    if(t eq null) null
    else {
      val k = t.key
      val v = t.value
      val l = t.left
      val r = t.right
      val l2 = transform(l, f)
      val v2 = f(k, v)
      val r2 = transform(r, f)
      if((v2.asInstanceOf[AnyRef] eq v.asInstanceOf[AnyRef])
          && (l2 eq l)
          && (r2 eq r)) t.asInstanceOf[Tree[A, C]]
      else mkTree(isBlackTree(t), k, v2, l2, r2)
    }

  def filterEntries[A, B](t: Tree[A, B], f: (A, B) => Boolean): Tree[A, B] = if(t eq null) null else {
    def fk(t: Tree[A, B]): Tree[A, B] = {
      val k = t.key
      val v = t.value
      val l = t.left
      val r = t.right
      val l2 = if(l eq null) null else fk(l)
      val keep = f(k, v)
      val r2 = if(r eq null) null else fk(r)
      if(!keep) join2(l2, r2)
      else if((l2 eq l) && (r2 eq r)) t
      else join(l2, k, v, r2)
    }
    blacken(fk(t))
  }

  def filterKeys[A, B](t: Tree[A, B], f: A => Boolean): Tree[A, B] = if(t eq null) null else {
    def fk(t: Tree[A, B]): Tree[A, B] = {
      val k = t.key
      val l = t.left
      val r = t.right
      val l2 = if(l eq null) null else fk(l)
      val keep = f(k)
      val r2 = if(r eq null) null else fk(r)
      if(!keep) join2(l2, r2)
      else if((l2 eq l) && (r2 eq r)) t
      else join(l2, k, t.value, r2)
    }
    blacken(fk(t))
  }

  def partitionEntries[A, B](t: Tree[A, B], p: (A, B) => Boolean): (Tree[A, B], Tree[A, B]) = if(t eq null) (null, null) else {
    var tmpk, tmpd = null: Tree[A, B] // shared vars to avoid returning tuples from fk
    def fk(t: Tree[A, B]): Unit = {
      val k = t.key
      val v = t.value
      val l = t.left
      val r = t.right
      var l2k, l2d, r2k, r2d = null: Tree[A, B]
      if(l ne null) {
        fk(l)
        l2k = tmpk
        l2d = tmpd
      }
      val keep = p(k, v)
      if(r ne null) {
        fk(r)
        r2k = tmpk
        r2d = tmpd
      }
      val jk =
        if(!keep) join2(l2k, r2k)
        else if((l2k eq l) && (r2k eq r)) t
        else join(l2k, k, v, r2k)
      val jd =
        if(keep) join2(l2d, r2d)
        else if((l2d eq l) && (r2d eq r)) t
        else join(l2d, k, v, r2d)
      tmpk = jk
      tmpd = jd
    }
    fk(t)
    (blacken(tmpk), blacken(tmpd))
  }

  def partitionKeys[A, B](t: Tree[A, B], p: A => Boolean): (Tree[A, B], Tree[A, B]) = if(t eq null) (null, null) else {
    var tmpk, tmpd = null: Tree[A, B] // shared vars to avoid returning tuples from fk
    def fk(t: Tree[A, B]): Unit = {
      val k = t.key
      val v = t.value
      val l = t.left
      val r = t.right
      var l2k, l2d, r2k, r2d = null: Tree[A, B]
      if(l ne null) {
        fk(l)
        l2k = tmpk
        l2d = tmpd
      }
      val keep = p(k)
      if(r ne null) {
        fk(r)
        r2k = tmpk
        r2d = tmpd
      }
      val jk =
        if(!keep) join2(l2k, r2k)
        else if((l2k eq l) && (r2k eq r)) t
        else join(l2k, k, v, r2k)
      val jd =
        if(keep) join2(l2d, r2d)
        else if((l2d eq l) && (r2d eq r)) t
        else join(l2d, k, v, r2d)
      tmpk = jk
      tmpd = jd
    }
    fk(t)
    (blacken(tmpk), blacken(tmpd))
  }


  // Based on Stefan Kahrs' Haskell version of Okasaki's Red&Black Trees
  // Constructing Red-Black Trees, Ralf Hinze: [[http://www.cs.ox.ac.uk/ralf.hinze/publications/WAAAPL99b.ps.gz]]
  // Red-Black Trees in a Functional Setting, Chris Okasaki: [[https://wiki.rice.edu/confluence/download/attachments/2761212/Okasaki-Red-Black.pdf]] */

  private[this] def del[A, B](tree: Tree[A, B], k: A)(implicit ordering: Ordering[A]): Tree[A, B] = if (tree eq null) null else {
    def delLeft =
      if (isBlackTree(tree.left)) balLeft(tree.key, tree.value, del(tree.left, k), tree.right)
      else RedTree(tree.key, tree.value, del(tree.left, k), tree.right)
    def delRight =
      if (isBlackTree(tree.right)) balRight(tree.key, tree.value, tree.left, del(tree.right, k))
      else RedTree(tree.key, tree.value, tree.left, del(tree.right, k))
    val cmp = ordering.compare(k, tree.key)
    if (cmp < 0) delLeft
    else if (cmp > 0) delRight
    else append(tree.left, tree.right)
  }

  private[this] def balance[A, B](x: A, xv: B, tl: Tree[A, B], tr: Tree[A, B]) =
    if (isRedTree(tl)) {
      if (isRedTree(tr)) RedTree(x, xv, tl.black, tr.black)
      else if (isRedTree(tl.left)) RedTree(tl.key, tl.value, tl.left.black, BlackTree(x, xv, tl.right, tr))
      else if (isRedTree(tl.right))
        RedTree(tl.right.key, tl.right.value, BlackTree(tl.key, tl.value, tl.left, tl.right.left), BlackTree(x, xv, tl.right.right, tr))
      else BlackTree(x, xv, tl, tr)
    } else if (isRedTree(tr)) {
      if (isRedTree(tr.right)) RedTree(tr.key, tr.value, BlackTree(x, xv, tl, tr.left), tr.right.black)
      else if (isRedTree(tr.left))
        RedTree(tr.left.key, tr.left.value, BlackTree(x, xv, tl, tr.left.left), BlackTree(tr.key, tr.value, tr.left.right, tr.right))
      else BlackTree(x, xv, tl, tr)
    } else BlackTree(x, xv, tl, tr)

  private[this] def balLeft[A, B](x: A, xv: B, tl: Tree[A, B], tr: Tree[A, B]) =
    if (isRedTree(tl)) RedTree(x, xv, tl.black, tr)
    else if (isBlackTree(tr)) balance(x, xv, tl, tr.red)
    else if (isRedTree(tr) && isBlackTree(tr.left))
      RedTree(tr.left.key, tr.left.value, BlackTree(x, xv, tl, tr.left.left), balance(tr.key, tr.value, tr.left.right, tr.right.red))
    else sys.error("Defect: invariance violation")

  private[this] def balRight[A, B](x: A, xv: B, tl: Tree[A, B], tr: Tree[A, B]) =
    if (isRedTree(tr)) RedTree(x, xv, tl, tr.black)
    else if (isBlackTree(tl)) balance(x, xv, tl.red, tr)
    else if (isRedTree(tl) && isBlackTree(tl.right))
      RedTree(tl.right.key, tl.right.value, balance(tl.key, tl.value, tl.left.red, tl.right.left), BlackTree(x, xv, tl.right.right, tr))
    else sys.error("Defect: invariance violation")

  /** `append` is similar to `join2` but requires that both subtrees have the same black height */
  private[this] def append[A, B](tl: Tree[A, B], tr: Tree[A, B]): Tree[A, B] =
    if (tl eq null) tr
    else if (tr eq null) tl
    else if (isRedTree(tl) && isRedTree(tr)) {
      val bc = append(tl.right, tr.left)
      if (isRedTree(bc)) {
        RedTree(bc.key, bc.value, RedTree(tl.key, tl.value, tl.left, bc.left), RedTree(tr.key, tr.value, bc.right, tr.right))
      } else {
        RedTree(tl.key, tl.value, tl.left, RedTree(tr.key, tr.value, bc, tr.right))
      }
    } else if (isBlackTree(tl) && isBlackTree(tr)) {
      val bc = append(tl.right, tr.left)
      if (isRedTree(bc)) {
        RedTree(bc.key, bc.value, BlackTree(tl.key, tl.value, tl.left, bc.left), BlackTree(tr.key, tr.value, bc.right, tr.right))
      } else {
        balLeft(tl.key, tl.value, tl.left, BlackTree(tr.key, tr.value, bc, tr.right))
      }
    } else if (isRedTree(tr)) RedTree(tr.key, tr.value, append(tl, tr.left), tr.right)
    else if (isRedTree(tl)) RedTree(tl.key, tl.value, tl.left, append(tl.right, tr))
    else sys.error("unmatched tree on append: " + tl + ", " + tr)


  // Bulk operations based on "Just Join for Parallel Ordered Sets" (https://www.cs.cmu.edu/~guyb/papers/BFS16.pdf)
  // We don't store the black height in the tree so we pass it down into the join methods and derive the black height
  // of child nodes from it. Where possible the black height is used directly instead of deriving the rank from it.
  // Our trees are supposed to have a black root so we always blacken as the last step of union/intersect/difference.

  def union[A, B](t1: Tree[A, B], t2: Tree[A, B])(implicit ordering: Ordering[A]): Tree[A, B] = blacken(_union(t1, t2))

  def intersect[A, B](t1: Tree[A, B], t2: Tree[A, B])(implicit ordering: Ordering[A]): Tree[A, B] = blacken(_intersect(t1, t2))

  def difference[A, B](t1: Tree[A, B], t2: Tree[A, _])(implicit ordering: Ordering[A]): Tree[A, B] =
    blacken(_difference(t1, t2.asInstanceOf[Tree[A, B]]))

  /** Compute the rank from a tree and its black height */
  @`inline` private[this] def rank(t: Tree[_, _], bh: Int): Int = {
    if(t eq null) 0
    else if(isBlackTree(t)) 2*(bh-1)
    else 2*bh-1
  }

  private[this] def joinRight[A, B](tl: Tree[A, B], k: A, v: B, tr: Tree[A, B], bhtl: Int, rtr: Int): Tree[A, B] = {
    val rtl = rank(tl, bhtl)
    if(rtl == (rtr/2)*2) RedTree(k, v, tl, tr)
    else {
      val tlBlack = isBlackTree(tl)
      val bhtlr = if(tlBlack) bhtl-1 else bhtl
      val ttr = joinRight(tl.right, k, v, tr, bhtlr, rtr)
      if(tlBlack && isRedTree(ttr) && isRedTree(ttr.right))
        RedTree(ttr.key, ttr.value,
          BlackTree(tl.key, tl.value, tl.left, ttr.left),
          ttr.right.black)
      else mkTree(tlBlack, tl.key, tl.value, tl.left, ttr)
    }
  }

  private[this] def joinLeft[A, B](tl: Tree[A, B], k: A, v: B, tr: Tree[A, B], rtl: Int, bhtr: Int): Tree[A, B] = {
    val rtr = rank(tr, bhtr)
    if(rtr == (rtl/2)*2) RedTree(k, v, tl, tr)
    else {
      val trBlack = isBlackTree(tr)
      val bhtrl = if(trBlack) bhtr-1 else bhtr
      val ttl = joinLeft(tl, k, v, tr.left, rtl, bhtrl)
      if(trBlack && isRedTree(ttl) && isRedTree(ttl.left))
        RedTree(ttl.key, ttl.value,
          ttl.left.black,
          BlackTree(tr.key, tr.value, ttl.right, tr.right))
      else mkTree(trBlack, tr.key, tr.value, ttl, tr.right)
    }
  }

  private[this] def join[A, B](tl: Tree[A, B], k: A, v: B, tr: Tree[A, B]): Tree[A, B] = {
    @tailrec def h(t: Tree[_, _], i: Int): Int =
      if(t eq null) i+1 else h(t.left, if(isBlackTree(t)) i+1 else i)
    val bhtl = h(tl, 0)
    val bhtr = h(tr, 0)
    if(bhtl > bhtr) {
      val tt = joinRight(tl, k, v, tr, bhtl, rank(tr, bhtr))
      if(isRedTree(tt) && isRedTree(tt.right)) tt.black
      else tt
    } else if(bhtr > bhtl) {
      val tt = joinLeft(tl, k, v, tr, rank(tl, bhtl), bhtr)
      if(isRedTree(tt) && isRedTree(tt.left)) tt.black
      else tt
    } else mkTree(isRedTree(tl) || isRedTree(tr), k, v, tl, tr)
  }

  private[this] def split[A, B](t: Tree[A, B], k: A)(implicit ordering: Ordering[A]): (Tree[A, B], Tree[A, B], Tree[A, B]) =
    if(t eq null) (null, null, null)
    else {
      val cmp = ordering.compare(k, t.key)
      if(cmp == 0) (t.left, t, t.right)
      else if(cmp < 0) {
        val (ll, b, lr) = split(t.left, k)
        (ll, b, join(lr, t.key, t.value, t.right))
      } else {
        val (rl, b, rr) = split(t.right, k)
        (join(t.left, t.key, t.value, rl), b, rr)
      }
    }

  private[this] def splitLast[A, B](t: Tree[A, B]): (Tree[A, B], A, B) =
    if(t.right eq null) (t.left, t.key, t.value)
    else {
      val (tt, kk, vv) = splitLast(t.right)
      (join(t.left, t.key, t.value, tt), kk, vv)
    }

  private[this] def join2[A, B](tl: Tree[A, B], tr: Tree[A, B]): Tree[A, B] =
    if(tl eq null) tr
    else if(tr eq null) tl
    else {
      val (ttl, k, v) = splitLast(tl)
      join(ttl, k, v, tr)
    }

  private[this] def _union[A, B](t1: Tree[A, B], t2: Tree[A, B])(implicit ordering: Ordering[A]): Tree[A, B] =
    if(t1 eq null) t2
    else if(t2 eq null) t1
    else {
      val (l1, _, r1) = split(t1, t2.key)
      val tl = _union(l1, t2.left)
      val tr = _union(r1, t2.right)
      join(tl, t2.key, t2.value, tr)
    }

  private[this] def _intersect[A, B](t1: Tree[A, B], t2: Tree[A, B])(implicit ordering: Ordering[A]): Tree[A, B] =
    if((t1 eq null) || (t2 eq null)) null
    else {
      val (l1, b, r1) = split(t1, t2.key)
      val tl = _intersect(l1, t2.left)
      val tr = _intersect(r1, t2.right)
      if(b ne null) join(tl, t2.key, t2.value, tr)
      else join2(tl, tr)
    }

  private[this] def _difference[A, B](t1: Tree[A, B], t2: Tree[A, B])(implicit ordering: Ordering[A]): Tree[A, B] =
    if((t1 eq null) || (t2 eq null)) t1
    else {
      val (l1, _, r1) = split(t1, t2.key)
      val tl = _difference(l1, t2.left)
      val tr = _difference(r1, t2.right)
      join2(tl, tr)
    }
}
