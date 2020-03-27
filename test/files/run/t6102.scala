// scalac: -opt:l:inline -opt-inline-from:** -Xfatal-warnings
//
// scala/bug#6102 Wrong bytecode in lazyval + no-op finally clause

object Test {

  def main(args: Array[String]): Unit = {
    try {
      val x = 3
    } finally {
      print("hello")
    }
  }
}

