package tipi.core

import org.scalatest._
import scala.util.parsing.combinator._

class ParserSuite extends FunSuite {
  import tipi.core.Parser._

  trait ParserTest {
    self: RegexParsers =>
    def runRule[T](rule: Parser[T], input: String): Option[T] = {
      self.parseAll(rule, input).map(Some(_)).getOrElse(None)
    }
  }

  val p1 = new Parser("{{", "}}") with ParserTest
  val p2 = new Parser("[:", ":]") with ParserTest

  test("id") {
    import p1._
    assert(runRule(id, "abc") === Some(Id("abc")))
    assert(runRule(id, "abc") === runRule(id, "ABC"))
    assert(runRule(id, "\"abc\"") === None)
    assert(runRule(id, "a.b.c") === Some(Id("a.b.c")))
    assert(runRule(id, "//") === None)
    assert(runRule(id, "a1") === Some(Id("a1")))
    assert(runRule(id, "1a") === None)
  }

  test("string") {
    import p1._
    assert(runRule(string, """ abc """.trim) === None)
    assert(runRule(string, """ "abc" """.trim) === Some(Text("abc")))
    assert(runRule(string, """ "a\\b\"c" """.trim) === Some(Text("a\\b\"c")))
    assert(runRule(string, """ "a\\b\"c" """.trim) === Some(Text("a\\b\"c")))
    assert(runRule(string, """ "a\b\"c" """.trim) === None)
    assert(runRule(string, """ "a\\b"c" """.trim) === None)
    assert(runRule(string, """ a.b.c """.trim) === None)
  }

  test("int") {
    import p1._
    assert(runRule(int, "123") === Some(Text("123")))
    assert(runRule(int, "-123") === Some(Text("-123")))
    assert(runRule(int, "") === None)
    assert(runRule(int, "-") === None)
    assert(runRule(int, "a") === None)
  }

  test("double") {
    import p1._
    assert(runRule(double, "123") === None)
    assert(runRule(double, "-123") === None)
    assert(runRule(double, "123.456") === Some(Text("123.456")))
    assert(runRule(double, "-123.456") === Some(Text("-123.456")))
    assert(runRule(double, ".123") === Some(Text(".123")))
    assert(runRule(double, "-.123") === Some(Text("-.123")))
    assert(runRule(double, "1.0") === Some(Text("1.0")))
    assert(runRule(double, "") === None)
    assert(runRule(double, "-") === None)
    assert(runRule(double, ".") === None)
    assert(runRule(double, "-.") === None)
    assert(runRule(double, "a") === None)
  }

  test("boolean") {
    import p1._
    assert(runRule(boolean, "true") === Some(Text("true")))
    assert(runRule(boolean, "false") === Some(Text("false")))
    assert(runRule(boolean, "a") === None)
  }

  test("arg") {
    import p1._
    assert(runRule(arg, "x=a") === Some(VariableArgument(Id("x"), Id("a"))))
    assert(runRule(arg, "x = a") === Some(VariableArgument(Id("x"), Id("a"))))
    assert(runRule(arg, "x=\"a\"") === Some(ConstantArgument(Id("x"), Text("a"))))
    assert(runRule(arg, "x = \"a\"") === Some(ConstantArgument(Id("x"), Text("a"))))
    assert(runRule(arg, "x=\"a\\\"b\"") === Some(ConstantArgument(Id("x"), Text("a\"b"))))
    assert(runRule(arg, "x = \"a\\\"b\"") === Some(ConstantArgument(Id("x"), Text("a\"b"))))
    assert(runRule(arg, "x=1") === Some(ConstantArgument(Id("x"), Text("1"))))
    assert(runRule(arg, "x = 1") === Some(ConstantArgument(Id("x"), Text("1"))))
    assert(runRule(arg, "x=1.0") === Some(ConstantArgument(Id("x"), Text("1.0"))))
    assert(runRule(arg, "x = 1.0") === Some(ConstantArgument(Id("x"), Text("1.0"))))
    assert(runRule(arg, "x = 1.") === Some(ConstantArgument(Id("x"), Text("1."))))
    assert(runRule(arg, "x=true") === Some(ConstantArgument(Id("x"), Text("true"))))
    assert(runRule(arg, "x = true") === Some(ConstantArgument(Id("x"), Text("true"))))
    assert(runRule(arg, "x") === Some(UnitArgument(Id("x"))))
  }

  test("argList") {
    import p1._
    assert(runRule(argList, "i=a j =1 k= 1.0 l m = \"a\" n  =  true") === Some(List(
      VariableArgument(Id("i"), Id("a")),
      ConstantArgument(Id("j"), Text("1")),
      ConstantArgument(Id("k"), Text("1.0")),
      UnitArgument(Id("l")),
      ConstantArgument(Id("m"), Text("a")),
      ConstantArgument(Id("n"), Text("true"))
    )))
  }

  test("simpleTag - {{ }}") {
    import p1._
    assert(runRule(simpleTag, "{{a}}") === Some(SimpleTag(Id("a"), Nil)))
    assert(runRule(simpleTag, "{{ a b=1 }}") === Some(SimpleTag(Id("a"), List(ConstantArgument(Id("b"), Text("1"))))))
    assert(runRule(simpleTag, "{{ a 1 }}") === None)
    assert(runRule(simpleTag, "[:a:]") === None)
  }

  test("openTag - {{ }}") {
    import p1._
    assert(runRule(openTag, "{{#a}}") === Some(OpenTag(Id("a"), Nil)))
    assert(runRule(openTag, "{{# a b=1 }}") === Some(OpenTag(Id("a"), List(ConstantArgument(Id("b"), Text("1"))))))
    assert(runRule(openTag, "{{# a 1 }}") === None)
  }

  test("closeTag - {{ }}") {
    import p1._
    assert(runRule(closeTag, "{{/a}}") === Some(CloseTag(Id("a"))))
    assert(runRule(closeTag, "{{/ abc }}") === Some(CloseTag(Id("abc"))))
    assert(runRule(closeTag, "{{/ a b=1 }}") === Some(CloseTag(Id("a"))))
  }

  test("simpleTag - [: :]") {
    import p2._
    assert(runRule(simpleTag, "[:a:]") === Some(SimpleTag(Id("a"), Nil)))
    assert(runRule(simpleTag, "[: a :]") === Some(SimpleTag(Id("a"), Nil)))
    assert(runRule(simpleTag, "[: a b=1 :]") === Some(SimpleTag(Id("a"), List(ConstantArgument(Id("b"), Text("1"))))))
    assert(runRule(simpleTag, "[: a 1 :]") === None)
    assert(runRule(simpleTag, "{{a}}") === None)
  }

  test("openTag - [: :]") {
    import p2._
    assert(runRule(openTag, "[:#a:]") === Some(OpenTag(Id("a"), Nil)))
    assert(runRule(openTag, "[:# a b=1 :]") === Some(OpenTag(Id("a"), List(ConstantArgument(Id("b"), Text("1"))))))
    assert(runRule(openTag, "[:# a 1 :]") === None)
  }

  test("closeTag - [: :]") {
    import p2._
    assert(runRule(closeTag, "[:/a:]") === Some(CloseTag(Id("a"))))
    assert(runRule(closeTag, "[:/ a :]") === Some(CloseTag(Id("a"))))
    assert(runRule(closeTag, "[:/ a 1 :]") === None)
  }

  test("text - {{ }}") {
    import p1._
    assert(runRule(text, "abc") === Some(Text("abc")))
    assert(runRule(text, "abc{") === Some(Text("abc{")))
    assert(runRule(text, "abc{ {") === Some(Text("abc{ {")))
    assert(runRule(text, "abc{{") === None)
    assert(runRule(text, " x ") === Some(Text(" x ")))
    assert(runRule(text, "abc[") === Some(Text("abc[")))
    assert(runRule(text, "abc[:") === Some(Text("abc[:")))
    assert(runRule(text, "a\nb") === Some(Text("a\nb")))
  }

  test("text - [: :]") {
    import p2._
    assert(runRule(text, "abc") === Some(Text("abc")))
    assert(runRule(text, "abc{") === Some(Text("abc{")))
    assert(runRule(text, "abc{ {") === Some(Text("abc{ {")))
    assert(runRule(text, "abc{{") === Some(Text("abc{{")))
    assert(runRule(text, " x ") === Some(Text(" x ")))
    assert(runRule(text, "abc[") === Some(Text("abc[")))
    assert(runRule(text, "abc[:") === None)
  }

  test("block - {{ }}") {
    import p1._
    assert(runRule(block, "abc") === Some(Text("abc")))
    assert(runRule(block, "{{ abc }}") === Some(Block(Id("abc"))))
    assert(runRule(block, "{{# abc }}") === None)
    assert(runRule(block, "{{# abc }}{{/ abc }}") === Some(Block(Id("abc"))))
    assert(runRule(block, "{{# abc }}{{/ def }}") === None)
    assert(runRule(block, "{{# abc }}{{ def }}{{/ abc }}") === Some(
      Block(
        Id("abc"),
        Arguments.Empty,
        Range(List(Block(Id("def"))))
      )
    ))
    assert(runRule(block, "{{# abc }}{{ def }}{{ ghi }}{{/ abc }}") === Some(
      Block(
        Id("abc"),
        Arguments.Empty,
        Range(List(
          Block(Id("def")),
          Block(Id("ghi"))
        ))
      )
    ))
    assert(runRule(block, "{{# abc }} x {{/ abc }}") === Some(
      Block(
        Id("abc"),
        Arguments.Empty,
        Range(List(
          Text(" x ")
        ))
      )
    ))
    assert(runRule(block, " {{# abc }} x {{ def }} y {{/ abc }} ") === None)
    assert(runRule(block, "[:# abc :] x [:/ abc :]") === Some(Text("[:# abc :] x [:/ abc :]")))
    // Newlines:
    assert(runRule(block, "[:# abc :]\nx\n[:/ abc :]") === Some(Text("[:# abc :]\nx\n[:/ abc :]")))
  }

  test("block - [: :]") {
    import p2._
    assert(runRule(block, "{{# abc }} x {{/ abc }}") === Some(Text("{{# abc }} x {{/ abc }}")))
    assert(runRule(block, "[:# abc :] x [:/ abc :]") === Some(
      Block(
        Id("abc"),
        Arguments.Empty,
        Range(List(
          Text(" x ")
        ))
      )
    ))
  }

  test("doc - {{ }}") {
    import p1._
    assert(runRule(doc, " {{# abc }} x {{ def }} y {{/ abc }} ") === Some(Range(List(
      Text(" "),
      Block(Id("abc"), Arguments.Empty, Range(List(
        Text(" x "),
        Block(Id("def")),
        Text(" y "))
      )),
      Text(" ")
    ))))
    assert(runRule(doc, " {{# abc }} x [: def :] y {{/ abc }} ") === Some(Range(List(
      Text(" "),
      Block(Id("abc"), Arguments.Empty, Range(List(
        Text(" x [: def :] y ")
      ))),
      Text(" ")
    ))))
  }

  test("doc - [: :]") {
    import p2._
    assert(runRule(doc, " {{# abc }} x [: def :] y {{/ abc }} ") === Some(Range(List(
      Text(" {{# abc }} x "),
      Block(Id("def")),
      Text(" y {{/ abc }} ")
    ))))
  }
}