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

/**
  * Annotation for specifying the `serialVersionUID` field of a (serializable) class.
  *
  * On the JVM, a class with this annotation will receive a `private`, `static`,
  * and `final` field called `serialVersionUID` with the provided `value`,
  * which the JVM's serialization mechanism uses to determine serialization
  * compatibility between different versions of a class.
  *
  * @see [[http://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html `java.io.Serializable`]]
  * @see [[Serializable]]
  */
@deprecatedInheritance("Scheduled for being final in 2.14", "2.13.0")
class SerialVersionUID(value: Long) extends scala.annotation.ConstantAnnotation
