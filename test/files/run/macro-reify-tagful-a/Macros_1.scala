import scala.language.experimental.macros
import scala.reflect.runtime.universe._
import scala.reflect.macros.blackbox.Context

object Macros {
  def foo[T](s: T): List[T] = macro Impls.foo[T]

  object Impls {
    def foo[T: c.WeakTypeTag](c: Context)(s: c.Expr[T]) = c.universe.reify {
      List(s.splice)
    }
  }
}
