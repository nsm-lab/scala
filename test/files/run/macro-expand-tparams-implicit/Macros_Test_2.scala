import scala.language.experimental.macros
object Test extends App {
  def foo[U](x: U): Unit = macro Impls.foo[U]
  foo(42)
  foo("42")
}
