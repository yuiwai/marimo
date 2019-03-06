package com.yuiwai.marimo.cli

object Cmd {
  def apply(name: String): Cmd = Cmd(name, Nil)
  def apply(name: String, options: => Seq[OptLike]): Cmd = Cmd(CmdName(name), options)
  def parse(str: String)(implicit cmdSet: CmdSet): Either[ParseError, CmdLike] = {
    parseImpl(str).map(_.lift).map {
      case Piped(commands) => Piped(commands)
      case cmdLike: CmdLike => cmdLike
    }
  }
  private def parseImpl(str: String): Either[ParseError, AbstractCmd] = {
    import fastparse.{parse => fastParse, Parsed}
    val parser = new Parser {}
    fastParse(str, parser.abstractCmd(_)) match {
      case Parsed.Success(abstractCmd, _) => Right(abstractCmd)
      case Parsed.Failure(_, _, _) => Left(SyntaxError)
    }
  }
}

final case class AbstractCmd(cmdName: CmdName, args: Seq[String]) {
  def lift(implicit cmdSet: CmdSet): CmdLike = cmdSet.lift(cmdName, args)
}
object AbstractCmd {
  def apply(cmdName: String): AbstractCmd = AbstractCmd(CmdName(cmdName), Seq.empty)
  def apply(cmdName: String, args: => Seq[String]): AbstractCmd = AbstractCmd(CmdName(cmdName), args)
}

trait Parser {
  import fastparse._, NoWhitespace._
  def ws[_: P]: P[Unit] = P(CharIn(" ").rep)
  def abstractCmd[_: P]: P[AbstractCmd] = P(cmdName ~ (ws ~ arg).rep ~ End).map {
    case (cmdName: CmdName, args) => AbstractCmd(cmdName, args)
  }
  /* def piped[_: P] = P(cmd ~ (ws.? ~ "|" ~ ws.? ~ cmd).rep).map {
    case (x, xs) => Piped(x +: xs)
  } */
  def cmdName[_: P]: P[CmdName] = P((CharIn("a-zA-Z") ~ CharIn("a-zA-Z0-9").rep).!).map(CmdName)
  def arg[_: P]: P[String] = P(CharsWhile(_ != ' ')).!
}

sealed trait ParseError
case object SyntaxError extends ParseError
final case class UnknownCmdError(cmdName: CmdName) extends ParseError
object UnknownCmdError {
  def apply(cmdName: => String): UnknownCmdError = apply(CmdName(cmdName))
}
final case class RequiredOptError() extends ParseError
case object ParamNotFoundError extends ParseError

sealed trait Elem {
  def isPartial: Boolean
}

sealed trait CmdLike extends Elem {
  val name: CmdName
}
final case class PartialCmd(
  name: CmdName,
  options: Seq[OptLike] = Seq.empty,
  errors: Seq[ParseError] = Seq.empty
) extends CmdLike {
  val isPartial: Boolean = true
  def withOpt(opt: OptLike): PartialCmd = copy(options = options :+ opt)
  def withError(error: ParseError): PartialCmd = copy(errors = errors :+ error)
}
object PartialCmd {
  lazy val empty = PartialCmd(CmdName(""))
  def apply(name: String): PartialCmd = PartialCmd(CmdName(name))
}
final case class Cmd(name: CmdName, options: Seq[OptLike]) extends CmdLike {
  def isPartial: Boolean = options.exists(_.isPartial)
}
final case class CmdName(name: String) extends AnyVal
final case class Piped(commands: Seq[Cmd]) extends CmdLike {
  def isPartial: Boolean = commands.exists(_.isPartial)
  val name: CmdName = CmdName("")
}

sealed trait OptLike extends Elem {
  val name: OptName
}
final case class PartialOpt(name: OptName, error: ParseError) extends OptLike {
  def isPartial: Boolean = true
}
object PartialOpt {
  def apply(name: String, error: => ParseError): PartialOpt = PartialOpt(OptName(name), error)
}
final case class Opt(name: OptName, value: OptValue, isPartial: Boolean) extends OptLike
object Opt {
  def apply(name: String): Opt = Opt(name, "")
  def apply(name: String, value: String, isPartial: Boolean): Opt = Opt(OptName(name), OptValue(value), isPartial)
  def apply(name: String, value: String): Opt = Opt(name, value, isPartial = false)
}
final case class OptName(name: String)
final case class OptValue(value: String)

final case class CmdSet(cmdDefs: Seq[CmdDef]) {
  def lift(cmdName: CmdName, args: Seq[String]): CmdLike = cmdDefs.find(_.name == cmdName) match {
    case Some(cmdDef) => cmdDef.lift(args)
    case _ => PartialCmd(cmdName).withError(UnknownCmdError(cmdName))
  }
}
final case class CmdDef(name: CmdName, opts: Seq[OptDef]) {
  def lift(args: Seq[String]): CmdLike = lift(args, Seq.empty)
  def lift(args: Seq[String], options: Seq[OptLike]): CmdLike = if (args.isEmpty) {
    opts.foldLeft[Seq[ParseError]](options.collect { case PartialOpt(_, err) => err }) { (errors, optDef) =>
      if (optDef.require && !options.exists(_.name == optDef.name)) errors :+ RequiredOptError()
      else errors
    } match {
      case errors if errors.isEmpty => Cmd(name, options)
      case errors => PartialCmd(name, options, errors)
    }
  } else {
    opts.find(_.matching(args.head)) match {
      case Some(optDef) =>
        optDef.lift(args.head.replaceAll("^\\-+", ""), args.tail) match {
          case (o, t) => lift(t, options :+ o)
        }
      case _ => PartialCmd(name)
    }
  }
}
object CmdDef {
  def apply(cmdName: String): CmdDef = CmdDef(CmdName(cmdName), Seq.empty)
}
final case class OptDef(name: OptName, withParam: Boolean = false, require: Boolean = false) {
  def lift(head: String, tail: Seq[String]): (OptLike, Seq[String]) =
    if (withParam) {
      if (tail.isEmpty) (PartialOpt(name, ParamNotFoundError), tail)
      else (Opt(head, tail.head), tail.tail)
    } else (Opt(head), tail)
  def matching(str: String): Boolean = str.replaceAll("^\\-+", "") == name.name
}

