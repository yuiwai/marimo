package com.yuiwai.marimo.cli

import utest._

object CmdSpec extends TestSuite {
  val tests = Tests {
    implicit val cmds = CmdSet(Seq.empty)
    "parse" - {
      Cmd.parse("foo").right.get ==> Cmd("foo")

      "required option" - {

      }
    }
  }
}

object ParserSpec extends TestSuite {
  val tests = Tests {
    val parser = new Parser {}
    import parser._
    import fastparse._
    "cmdName" - {
      def valid(name: String) = parse(name, cmdName(_)).get.value ==> CmdName(name)
      def invalid(input: String) = parse(input, cmdName(_)).isSuccess ==> false
      valid("foo")
      valid("foo1")
      valid("FOO")
      invalid("")
      invalid("1foo")
    }
    "opt" - {
      def valid(input: String, name: String, value: String, isPartial: Boolean = false) = {
        parse(input, opt(_)).get.value ==> Opt(name, value, isPartial)
      }
      def invalid(input: String) = parse(input, opt(_)).isSuccess ==> false
      invalid("")
      invalid("f")
      invalid("foo")
      valid("-f", "f", "")
      valid("-f 1", "f", "1")
      valid("-foo", "f", "oo")
      valid("--foo", "foo", "")
      valid("--foo a,b,c", "foo", "a,b,c")
      valid("--foo bar", "foo", "bar")
    }
    "opts" - {
      parse("--f1 bar", opts(_)).get.value.toList ==> List(Opt("f1", "bar"))
      parse("--f1 bar --f2 baz", opts(_)).get.value.toList ==> List(Opt("f1", "bar"), Opt("f2", "baz"))
    }
    "cmd" - {
      parse("foo", cmd(_)).get.value ==> Cmd("foo")
      parse("foo -f", cmd(_)).get.value ==> Cmd("foo", List(Opt("f")))
      parse("foo -f bar", cmd(_)).get.value ==> Cmd("foo", List(Opt("f", "bar")))
      parse("foo -f bar -g baz", cmd(_)).get.value ==> Cmd("foo", List(Opt("f", "bar"), Opt("g", "baz")))
      parse("foo --bar", cmd(_)).get.value ==> Cmd("foo", List(Opt("bar")))
      parse("foo --bar baz", cmd(_)).get.value ==> Cmd("foo", List(Opt("bar", "baz")))
    }
    "piped" - {
      parse("foo|bar", piped(_)).get.value ==> Piped(Seq(Cmd("foo"), Cmd("bar")))
      parse("foo | bar", piped(_)).get.value ==> Piped(Seq(Cmd("foo"), Cmd("bar")))
      parse("foo -f 1 | bar", piped(_)).get.value ==> Piped(Seq(Cmd("foo", Seq(Opt("f", "1"))), Cmd("bar")))
    }
  }
}
