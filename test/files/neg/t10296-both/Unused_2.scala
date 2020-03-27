// scalac: -Werror -Xlint:unused -Wmacros:both
import scala.language.experimental.macros

object Unused extends App {
  def m(body: Int): Int = macro UnusedMacro.macroImpl

  private def g(): Int = 17
  private def k(): Int = 17

  // g is used before but not after expansion
  def f(): Int = m(g())

  println(f())
}
