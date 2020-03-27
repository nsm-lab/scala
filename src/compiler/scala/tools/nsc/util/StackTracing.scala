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

package scala.tools.nsc.util

import collection.mutable, mutable.ListBuffer
import java.lang.System.lineSeparator

private[util] trait StackTracing extends Any {

  /** Format a stack trace, returning the prefix consisting of frames that satisfy a given predicate.
   *
   *  The format is similar to the typical case described in the Javadoc
   *  for [[java.lang.Throwable#printStackTrace()*]].
   *  If a stack trace is truncated, it will be followed by a line of the form
   *  `... 3 elided`, by analogy to the lines `... 3 more` which indicate
   *  shared stack trace segments.
   *  @param e the exception
   *  @param p the predicate to select the prefix
   */
  def stackTracePrefixString(e: Throwable)(p: StackTraceElement => Boolean): String = {

    type TraceRelation = String
    val Self       = new TraceRelation("")
    val CausedBy   = new TraceRelation("Caused by: ")
    val Suppressed = new TraceRelation("Suppressed: ")

    def header(e: Throwable): String  = {
      def because(e: Throwable): String = e.getCause match { case null => null ; case c => header(c) }
      def msg(e: Throwable): String     = e.getMessage match { case null => because(e) ; case s => s }
      def txt(e: Throwable): String     = msg(e) match { case null => "" ; case s => s": $s" }
      s"${e.getClass.getName}${txt(e)}"
    }

    val seen = mutable.Set.empty[Throwable]
    def unseen(t: Throwable) = {
      val interesting = t != null && !seen(t)
      if (interesting) seen += t
      interesting
    }

    val lines = ListBuffer.empty[String]

    // format the stack trace, skipping the shared trace
    def print(e: Throwable, r: TraceRelation, share: Array[StackTraceElement], indents: Int): Unit = if (unseen(e)) {
      val trace  = e.getStackTrace
      val frames = if (share.isEmpty) trace else {
        val spare  = share.reverseIterator
        val trimmed = trace.reverse dropWhile (spare.hasNext && spare.next == _)
        trimmed.reverse
      }
      val prefix = frames.takeWhile(p)
      val margin = "  " * indents
      lines += s"${margin}${r}${header(e)}"
      prefix.foreach(f => lines += s"${margin}  at $f")
      if (frames.size < trace.size) lines += s"${margin}  ... ${trace.size - frames.size} more"
      if (r == Self && prefix.size < frames.size) lines += s"${margin}  ... ${frames.size - prefix.size} elided"
      print(e.getCause, CausedBy, trace, indents)
      e.getSuppressed.foreach(print(_, Suppressed, frames, indents + 1))
    }
    print(e, Self, share = Array.empty, indents = 0)
    lines.mkString(lineSeparator)
  }
}
