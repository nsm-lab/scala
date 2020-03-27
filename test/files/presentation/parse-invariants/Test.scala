import scala.tools.nsc.interactive.tests.InteractiveTest
import scala.reflect.internal.util.SourceFile
import scala.tools.nsc.interactive.Response

object Test extends InteractiveTest {

  override def execute(): Unit = {
    def test(fileName: String): Unit = {
      val sf = sourceFiles.find(_.file.name == fileName).get
      noNewSymbols(sf)
      uniqueParseTree(sf)
      unattributedParseTree(sf)
      neverModifyParseTree(sf)
      shouldAlwaysReturnParseTree(sf)
    }
    test("A.scala")
    test("A.java")
  }

  /**
   * Asking for a parseTree should not enter any new symbols.
   */
  private def noNewSymbols(sf: SourceFile): Unit = {
    def nextId() = compiler.NoSymbol.newTermSymbol(compiler.TermName("dummy"), compiler.NoPosition, compiler.NoFlags).id
    val id = nextId()
    println("parseTree")
    val tree = compiler.parseTree(sf)
    val id2 = nextId()
    if (id2 == id + 1) {
      reporter.println("NoNewSymbolsEntered OK")
    } else {
      reporter.println("NoNewSymbolsEntered FAILED")
    }
  }

  /**
   * Asking twice for a parseTree on the same source should always return a new tree
   */
  private def uniqueParseTree(sf: SourceFile): Unit = {
    val parseTree1 = compiler.parseTree(sf)
    val parseTree2 = compiler.parseTree(sf)
    if (parseTree1 != parseTree2) {
      reporter.println("Unique OK")
    } else {
      reporter.println("Unique FAILED")
    }
  }

  /**
   * A parseTree should never contain any symbols or types
   */
  private def unattributedParseTree(sf: SourceFile): Unit = {
    if (noSymbolsOrTypes(compiler.parseTree(sf))) {
      reporter.println("Unattributed OK")
    } else {
      reporter.println("Unattributed FAILED")
    }
  }

  /**
   * Once you have obtained a parseTree it should never change
   */
  private def neverModifyParseTree(sf: SourceFile): Unit = {
    val parsedTree = compiler.parseTree(sf)
    loadSourceAndWaitUntilTypechecked(sf)
    if (noSymbolsOrTypes(parsedTree)) {
      reporter.println("NeverModify OK")
    } else {
      reporter.println("NeverModify FAILED")
    }
  }

  /**
   * Should always return a parse tree
   */
   private def shouldAlwaysReturnParseTree(sf: SourceFile): Unit = {
     loadSourceAndWaitUntilTypechecked(sf)
     if (noSymbolsOrTypes(compiler.parseTree(sf))) {
       reporter.println("AlwaysParseTree OK")
     } else {
       reporter.println("AlwaysParseTree FAILED")
     }
   }

  /**
   * Load a source and block while it is type-checking.
   */
  private def loadSourceAndWaitUntilTypechecked(sf: SourceFile): Unit = {
    compiler.askToDoFirst(sf)
    val res = new Response[Unit]
    compiler.askReload(List(sf), res)
    res.get
    askLoadedTyped(sf).get
  }

  /**
   * Traverses a tree and makes sure that there are no types or symbols present in the tree with
   * the exception of the symbol for the package 'scala'. This is because that symbol will be
   * present in some of the nodes that the compiler generates.
   */
  private def noSymbolsOrTypes(tree: compiler.Tree): Boolean = {
    tree.forAll { t =>
      (t.symbol == null ||
       t.symbol == compiler.NoSymbol ||
       t.symbol == compiler.definitions.ScalaPackage // ignore the symbol for the scala package for now
      ) && (
       t.tpe == null ||
       t.tpe == compiler.NoType)
    }
  }

}
