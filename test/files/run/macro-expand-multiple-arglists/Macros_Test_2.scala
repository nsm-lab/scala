import scala.language.experimental.macros
object Test extends App {
  def foo(x: Int)(y: Int): Unit = macro Impls.foo
  foo(40)(2)
}
