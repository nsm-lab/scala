import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object Impls {
  def foo(c: Context) = {
    import c.universe._
    val body = Ident(TermName("IDoNotExist"))
    c.Expr[Int](body)
  }
}

object Macros {
  def foo: Int = macro Impls.foo
}
