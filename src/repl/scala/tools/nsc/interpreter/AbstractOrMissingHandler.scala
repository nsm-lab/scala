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

package scala.tools.nsc.interpreter

import scala.reflect.internal.MissingRequirementError

class AbstractOrMissingHandler[T](onError: String => Unit, value: T) extends PartialFunction[Throwable, T] {
  def isDefinedAt(t: Throwable) = t match {
    case _: AbstractMethodError     => true
    case _: NoSuchMethodError       => true
    case _: MissingRequirementError => true
    case _: NoClassDefFoundError    => true
    case _                          => false
  }
  def apply(t: Throwable) = t match {
    case e @ (_: AbstractMethodError | _: NoSuchMethodError | _: NoClassDefFoundError) =>
      onError(s"""
        |Failed to initialize compiler: ${e.getClass.getName.split('.').last}.
        |This is most often remedied by a full clean and recompile.
        |Otherwise, your classpath may continue bytecode compiled by
        |different and incompatible versions of scala.
        |""".stripMargin
      )
      e.printStackTrace()
      value
    case e: MissingRequirementError =>
      onError(s"""
        |Failed to initialize compiler: ${e.req} not found.
        |** Note that as of 2.8 scala does not assume use of the java classpath.
        |** For the old behavior pass -usejavacp to scala, or if using a Settings
        |** object programmatically, settings.usejavacp.value = true.""".stripMargin
      )
      value
  }
}

object AbstractOrMissingHandler {
  def apply[T]() = new AbstractOrMissingHandler[T](Console println _, null.asInstanceOf[T])
}
