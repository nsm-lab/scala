import scala.language.experimental.macros
object Macros {
  def foo: Unit = macro Impls.foo[String]
}

object Test extends App {
  import Macros._
  foo
}
