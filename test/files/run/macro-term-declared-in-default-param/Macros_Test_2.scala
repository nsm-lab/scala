import scala.language.experimental.macros
object Test extends App {
  def foo(bar: String = { def foo: String = macro Impls.foo; foo }) = println(bar)

  foo()
  foo("it works")
  foo()
}
