import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object Impls {
  def foo(unconventionalName: Context)(x: unconventionalName.Expr[Int]) = {
    import unconventionalName.universe._
    unconventionalName.Expr[Unit](q"""println("invoking foo...")""")
  }
}

object Macros {
  def foo(x: Int): Unit = macro Impls.foo
}
