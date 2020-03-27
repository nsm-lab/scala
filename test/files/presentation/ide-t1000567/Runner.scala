import scala.tools.nsc.interactive.tests.InteractiveTest

// also known as scala/bug#5013

object Test extends InteractiveTest {

  override def runDefaultTests(): Unit = {
    val a = sourceFiles.find(_.file.name == "a.scala").get
    val b = sourceFiles.find(_.file.name == "b.scala").get
    askLoadedTyped(a).get
    askLoadedTyped(b).get
    super.runDefaultTests()
  }

}
