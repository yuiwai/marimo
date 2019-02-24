package com.yuiwai.marimo.cli

object Cmd {
  def apply(name: String): Cmd = Cmd(name, Nil)
  def apply(name: String, options: => Seq[Opt]): Cmd = Cmd(CmdName(name), options)
  def parse(str: String)(implicit cmdSet: CmdSet): Either[ParseError, CmdLike] = parseImpl(str)
  private def parseImpl(str: String): Either[ParseError, CmdLike] = {
    import fastparse.{parse => fastParse, Parsed}
    val parser = new Parser {}
    fastParse(str, parser.piped(_)) match {
      case Parsed.Success(piped, _) => Right(piped)
      case Parsed.Failure(_, _, _) => Left(SyntaxError)
    }
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
case object SyntaxError extends ParseError {
  def cmd: PartialCmd = PartialCmd.empty
}

sealed trait Elem {
  def isPartial: Boolean
}

sealed trait CmdLike extends Elem {
  val name: CmdName
}
final case class PartialCmd(name: CmdName, options: Seq[Opt] = Seq.empty) extends CmdLike {
  val isPartial: Boolean = true
}
object PartialCmd {
  lazy val empty = PartialCmd(CmdName(""))
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

