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

package scala.tools.nsc.interpreter.shell

import java.io.IOException

/** Reads lines from an input stream */
trait InteractiveReader {
  def interactive: Boolean

  def accumulator: Accumulator

  def reset(): Unit
  def history: History
  def completion: Completion
  def redrawLine(): Unit

  def readYesOrNo(prompt: String, alt: => Boolean): Boolean = readOneKey(prompt) match {
    case 'y'  => true
    case 'n'  => false
    case -1   => false // EOF
    case _    => alt
  }

  protected def readOneLine(prompt: String): String
  protected def readOneKey(prompt: String): Int

  def readLine(prompt: String): String = readOneLine(prompt)
    /*
    // hack necessary for OSX jvm suspension because read calls are not restarted after SIGTSTP
    if (scala.util.Properties.isMac) restartSysCalls(readOneLine(prompt), reset())
    else readOneLine(prompt)
    */

  @deprecated("No longer used", "2.13.1")
  def initCompletion(completion: Completion): Unit = ()
}

object InteractiveReader {
  val msgEINTR = "Interrupted system call"
  def restartSysCalls[R](body: => R, reset: => Unit): R =
    try body catch {
      case e: IOException if e.getMessage == msgEINTR => reset ; body
    }

  def apply(): InteractiveReader = SimpleReader()
}

/** Accumulate multi-line input. Shared by Reader and Completer, which must parse accumulated result. */
class Accumulator {
  var text: List[String] = Nil
  def reset(): Unit = text = Nil
  def +=(s: String): Unit = text :+= s
  override def toString = text.mkString("\n")
}
