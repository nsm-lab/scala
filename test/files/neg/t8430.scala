// scalac: -Xfatal-warnings -Ypatmat-exhaust-depth off
//
sealed trait CL3Literal
case object IntLit extends CL3Literal
case object CharLit extends CL3Literal
case object BooleanLit extends CL3Literal
case object UnitLit extends CL3Literal
 
 
sealed trait Tree
case class LetL(value: CL3Literal) extends Tree
case object LetP extends Tree
case object LetC extends Tree
case object LetF extends Tree
 
object Test {
  val f0 = (tree: Tree) => tree match {case LetL(CharLit) => ??? }
  val f1 = (tree: Tree) => tree match {case LetL(CharLit) => ??? }
  val f2 = (tree: Tree) => tree match {case LetL(CharLit) => ??? }
  val f3 = (tree: Tree) => tree match {case LetL(CharLit) => ??? }
  val f4 = (tree: Tree) => tree match {case LetL(CharLit) => ??? }
  val f5 = (tree: Tree) => tree match {case LetL(CharLit) => ??? }
  // After the first patch for scala/bug#8430, we achieve stability: all of
  // these get the same warning:
  //
  // ??, LetC, LetF, LetL(IntLit), LetP
  //
  // Before, it was non-deterministic.
  //
  // However, we our list of counter examples is itself non-exhaustive.
  // We need to rework counter example generation to fix that.
  //
  // That work is the subject of scala/bug#7746
}
