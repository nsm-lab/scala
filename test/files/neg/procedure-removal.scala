// scalac: -Xsource:2.14
//
abstract class Foo {
  def bar {}
  def baz
  def boo(i: Int, l: Long)
  def boz(i: Int, l: Long) {}
  def this(i: Int) { this() } // Don't complain here! Just slap them with an error.
  def foz: Unit               // Don't complain here!
}
