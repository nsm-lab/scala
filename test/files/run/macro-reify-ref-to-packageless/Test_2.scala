import scala.language.experimental.macros
object Test extends App {
  def foo: Int = macro Impls.foo
  println(foo)
}
