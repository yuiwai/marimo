package com.yuiwai.marimo.cli

object Cmd {
  def apply(name: String): Cmd = Cmd(name, Nil)
  def apply(name: String, options: => Seq[Opt]): Cmd = Cmd(CmdName(name), options)
  def parse(str: String)(implicit cmdSet: CmdSet): Either[ParseError, Cmd] = parseImpl(str)
  private def parseImpl(str: String): Either[ParseError, Cmd] = {
    Right(Cmd(CmdName(str), Nil))
  }
}

trait Parser {
  import fastparse._, NoWhitespace._
  def ws[_: P] = P(CharIn(" ").rep)
  def piped[_: P] = P(cmd ~ (ws.? ~ "|" ~ ws.? ~ cmd).rep).map {
    case (x, xs) => Piped(x +: xs)
  }
  def cmd[_: P] = P(cmdName ~ ws.? ~ opts).map {
    case (name, opts) => Cmd(name, opts)
  }
  def cmdName[_: P] = P((CharIn("a-zA-Z") ~ CharIn("a-zA-Z0-9").rep).!).map(CmdName)
  def opts[_: P] = P((opt ~ ws.?).rep)
  def opt[_: P] = P(shortOpt | longOpt).map {
    case (name, value) => Opt(OptName(name), OptValue(value.getOrElse("")), isPartial = false)
  }
  def shortOpt[_: P] = P("-" ~ CharIn("a-zA-Z").! ~ (ws ~ optV).?)
  def longOpt[_: P] = P("--" ~ (CharIn("a-zA-Z") ~ CharIn("a-zA-Z0-9").rep).! ~ (ws ~ optV).?)
  def optV[_: P] = P(CharsWhile(_ != ' ')).!
}

sealed trait ParseError {
  def cmd: PartialCmd
}

sealed trait Elem {
  def isPartial: Boolean
}

sealed trait CmdLike extends Elem {
  val name: CmdName
}
final case class PartialCmd(name: CmdName) extends CmdLike {
  val isPartial: Boolean = true
}
final case class Cmd(name: CmdName, options: Seq[Opt]) extends CmdLike {
  def isPartial: Boolean = options.exists(_.isPartial)
}
final case class CmdName(name: String) extends AnyVal
final case class Piped(commands: Seq[Cmd]) extends CmdLike {
  def isPartial: Boolean = commands.exists(_.isPartial)
  val name: CmdName = CmdName("")
}

final case class Opt(name: OptName, value: OptValue, isPartial: Boolean) extends Elem
object Opt {
  def apply(name: String): Opt = Opt(name, "")
  def apply(name: String, value: String, isPartial: Boolean): Opt = Opt(OptName(name), OptValue(value), isPartial)
  def apply(name: String, value: String): Opt = Opt(name, value, isPartial = false)
}
final case class OptName(name: String)
final case class OptValue(value: String)

final case class CmdDef(name: CmdName, opts: Seq[OptDef])
final case class OptDef(name: OptName)
final case class CmdSet(cmds: Seq[CmdDef])

