
trait t6810 {
  val x = '\u000A'    // char literals accept arbitrary unicode escapes
  val y = '
'                     // but not embedded EOL sequences not represented as escapes
  val z = '\n'        // normally, expect this escape

  val X = "\u000A"    // it's the same as ordinary string literals
  val Y = "
"                     // obviously not
  val Z = "\n"        // normally, expect this escape

  val A = """
"""                   // which is what these are for
  val B = s"""
"""                   // or the same for interpolated strings

  import System.{lineSeparator => EOL}
  val `\u000A` = EOL  // backquoted identifiers are arbitrary string literals
  val `
` = EOL               // not raw string literals aka triple-quoted, multiline strings

  val firebreak = 42  // help parser recovery, could also use rbrace

  val a = '\u000D'    // similar treatment of CR
  val b = ''        // CR seen as EOL by scanner
  val c = '\r'        // traditionally
}
