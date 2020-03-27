import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

trait Macro_T {
 def foo[T](c: Context)(s: c.Expr[T]) = s
}

object Macros {
  def foo[T](s: T): T = macro Impls.foo[T]
  object Impls extends Macro_T
}
