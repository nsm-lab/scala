// scalac: -opt:l:none
//
// scala/bug#7407
object Test {

  def main(args: Array[String]): Unit = { println(foo) }

  def foo: String = {
    try return "Hello" finally 10 match {case x => ()}
  }

}

