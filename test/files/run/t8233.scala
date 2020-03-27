object Test {
  def bar(s: String) = s;
  val o: Option[Null] = None
  def nullReference: Unit = {
    val a: Null = o.get
    bar(a) // Was: VerifyError under GenICode
  }

  def literal: Unit = {
    val a: Null = null
    bar(a)
  }

  /** Check scala/bug#8330 for details */
  def expectedUnitInABranch(b: Boolean): Boolean = {
    if (b) {
      val x = 12
      ()
    } else {
      // here expected type is (unboxed) Unit
      null
    }
    true
  }

  def main(args: Array[String]): Unit = {
    try { nullReference } catch { case _: NoSuchElementException => }
    literal
    expectedUnitInABranch(true) // Was: VerifyError under GenICode
  }
}
