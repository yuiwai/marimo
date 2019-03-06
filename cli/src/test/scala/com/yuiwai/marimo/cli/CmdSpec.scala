package com.yuiwai.marimo.cli

import utest._

object CmdSpec extends TestSuite {
  val tests = Tests {
    implicit val cmdSet: CmdSet = CmdSet(Seq(
      CmdDef("foo")
    ))
    "parse" - {
      "valid command name" - {
        Cmd.parse("foo").right.get ==> Cmd("foo")
      }
      "invalid command name" - {
        Cmd.parse("bar").right.get ==> PartialCmd("bar").withError(UnknownCmdError("bar"))
      }
      "required option without param" - {
      }
      "required option with param" - {
      }
    }
  }
}

object AbstractCmdSpec extends TestSuite {
  val tests = Tests {
    "lift" - {
      "without required option" - {
        implicit val cmdSet: CmdSet = CmdSet(
          Seq(
            CmdDef(
              CmdName("foo"),
              Seq(
                OptDef(OptName("f")),
                OptDef(OptName("b"), withParam = true)
              )
            )
          )
        )
        AbstractCmd("foo").lift ==> Cmd("foo")
        AbstractCmd("foo", Seq("-f")).lift ==> Cmd("foo", Seq(Opt("f")))
        AbstractCmd("foo", Seq("-b", "bar")).lift ==> Cmd("foo", Seq(Opt("b", "bar")))
        AbstractCmd("foo", Seq("-f", "-b", "bar")).lift ==> Cmd("foo", Seq(Opt("f"), Opt("b", "bar")))
        AbstractCmd("foo", Seq("-b", "bar", "-f")).lift ==> Cmd("foo", Seq(Opt("b", "bar"), Opt("f")))
        AbstractCmd("foo", Seq("-b")).lift ==> PartialCmd("foo")
          .withOpt(PartialOpt("b", ParamNotFoundError))
          .withError(ParamNotFoundError)
        AbstractCmd("foo", Seq("-f", "-b")).lift ==> PartialCmd("foo")
          .withOpt(Opt("f"))
          .withOpt(PartialOpt("b", ParamNotFoundError))
          .withError(ParamNotFoundError)
      }
      "with required option" - {
        implicit val cmdSet: CmdSet = CmdSet(
          Seq(
            CmdDef(
              CmdName("foo"),
              Seq(
                OptDef(OptName("f"), require = true),
                OptDef(OptName("b"))
              )
            )
          )
        )
        AbstractCmd("foo").lift ==> PartialCmd("foo").withError(RequiredOptError())
        AbstractCmd("foo", Seq("-f")).lift ==> Cmd("foo", Seq(Opt("f")))
        AbstractCmd("foo", Seq("-f", "-b")).lift ==> Cmd("foo", Seq(Opt("f"), Opt("b")))
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
      def valid(name: String): Unit = parse(name, cmdName(_)).get.value ==> CmdName(name)

      def invalid(input: String): Unit = parse(input, cmdName(_)).isSuccess ==> false

      valid("foo")
      valid("foo1")
      valid("FOO")
      invalid("")
      invalid(" foo")
      invalid("1foo")
    }
    "abstractCmd" - {
      "only cmdName" - {
        parse("foo", abstractCmd(_)).get.value ==> AbstractCmd("foo")
      }
      "with opt" - {
        parse("foo -f", abstractCmd(_)).get.value ==> AbstractCmd("foo", Seq("-f"))
        parse("foo -f 1", abstractCmd(_)).get.value ==> AbstractCmd("foo", Seq("-f", "1"))
        parse("foo -f 1,2,3", abstractCmd(_)).get.value ==> AbstractCmd("foo", Seq("-f", "1,2,3"))
        parse("foo -f 1.2", abstractCmd(_)).get.value ==> AbstractCmd("foo", Seq("-f", "1.2"))
        parse("foo -f 1 --bar", abstractCmd(_)).get.value ==> AbstractCmd("foo", Seq("-f", "1", "--bar"))
      }
    }
  }
}
