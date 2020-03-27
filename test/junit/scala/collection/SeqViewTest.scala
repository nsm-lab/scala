package scala.collection

import org.junit.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(classOf[JUnit4])
class SeqViewTest {
  @Test
  def _toString(): Unit = {
    assertEquals("SeqView(<not computed>)", Seq(1, 2, 3).view.toString)
  }
}
