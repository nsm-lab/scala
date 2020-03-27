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

package scala.annotation

/** A method annotation which suppresses the creation of
 *  additional specialized forms based on enclosing specialized
 *  type parameters.
 */
@deprecatedInheritance("Scheduled for being final in 2.14", "2.13.0")
class unspecialized extends scala.annotation.StaticAnnotation
