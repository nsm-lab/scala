import scala.language.experimental.macros
object Macros {
  def foo[U <: String]: Unit = macro Impls.foo[U]
}

object Test extends App {
  import Macros._
  foo[Int]
}
