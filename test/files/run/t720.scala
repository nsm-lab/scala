class Lazy(f: => Int) {
  lazy val get: Int = f
}

class UsedLater(f: => Int) {
  lazy val get: Int = f
  def other = f
}

class TransientLazy(f: => Int) {
  @transient
  lazy val get: Int = f
}

object Test {
  def main(args: Array[String]): Unit = {
    testLazy()
    testUsedLater()
  }

  def testLazy(): Unit = {
    val o = new Lazy("".length)
    val f = classOf[Lazy].getDeclaredField("f")
    f.setAccessible(true)
    assert(f.get(o) != null)
    o.get
    assert(f.get(o) == null)
  }

  def testUsedLater(): Unit = {
    val o = new UsedLater("".length)
    val f = classOf[UsedLater].getDeclaredField("f")
    f.setAccessible(true)
    assert(f.get(o) != null)
    o.get
    assert(f.get(o) != null)
  }

  def testTransientLazy(): Unit = {
    val o = new TransientLazy("".length)
    val f = classOf[TransientLazy].getDeclaredField("f")
    f.setAccessible(true)
    assert(f.get(o) != null)
    o.get
    assert(f.get(o) != null) // scala/bug#9365
  }
}

