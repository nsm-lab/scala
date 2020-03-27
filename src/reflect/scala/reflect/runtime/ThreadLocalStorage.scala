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

package scala.reflect
package runtime

import java.lang.Thread._

private[reflect] trait ThreadLocalStorage {
  self: SymbolTable =>

  // see a discussion at scala-internals for more information:
  // http://groups.google.com/group/scala-internals/browse_thread/thread/337ce68aa5e51f79
  trait ThreadLocalStorage[T] { def get: T; def set(newValue: T): Unit }
  private class MyThreadLocalStorage[T](initialValue: => T) extends ThreadLocalStorage[T] {
    // TODO: how do we use org.cliffc.high_scale_lib.NonBlockingHashMap here?
    //       (we would need a version that uses weak keys)
    private[this] val values = java.util.Collections.synchronizedMap(new java.util.WeakHashMap[Thread, T]())
    def get: T = {
      if (values containsKey currentThread) values.get(currentThread)
      else {
        val value = initialValue
        // since the key is currentThread, and `values` is private, it
        // would be impossible for a value to have been set after the
        // above containsKey check. `putIfAbsent` is not necessary.
        values.put(currentThread, value)
        value
      }
    }
    def set(newValue: T): Unit = {
      values.put(currentThread, newValue)
    }
  }
  @inline final def mkThreadLocalStorage[T](x: => T): ThreadLocalStorage[T] = new MyThreadLocalStorage(x)
}
