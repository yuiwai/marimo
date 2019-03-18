package com.yuiwai.marimo.cli

object Cmd {
  def apply(name: String): Cmd = Cmd(name, Nil)
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

final case class AbstractCmd(name: String, inputs: Seq[String]) {
  def lift(implicit cmdSet: CmdSet): CmdLike = cmdSet.lift(name, inputs)
}
object AbstractCmd {
  def apply(name: String): AbstractCmd = AbstractCmd(name, Seq.empty)
}

trait Parser {
  import fastparse._, NoWhitespace._
  def ws[_: P]: P[Unit] = P(CharIn(" ").rep)
  def abstractCmd[_: P]: P[AbstractCmd] = P(cmdName ~ (ws ~ arg).rep ~ End).map {
    case (name: String, args) => AbstractCmd(name, args)
  }
  /* def piped[_: P] = P(cmd ~ (ws.? ~ "|" ~ ws.? ~ cmd).rep).map {
    case (x, xs) => Piped(x +: xs)
  } */
  def cmdName[_: P]: P[String] = P((CharIn("a-zA-Z") ~ CharIn("a-zA-Z0-9").rep).!)
  def arg[_: P]: P[String] = P(CharsWhile(_ != ' ')).!
}

sealed trait ParseError
case object SyntaxError extends ParseError
final case class UnknownCmdError(name: String) extends ParseError
final case class RequiredOptError() extends ParseError
case object ParamNotFoundError extends ParseError

sealed trait Elem {
  def isPartial: Boolean
}

sealed trait CmdLike extends Elem {
  val name: String
}
final case class PartialCmd(
  name: String,
  options: Seq[OptLike] = Seq.empty,
  errors: Seq[ParseError] = Seq.empty
) extends CmdLike {
  val isPartial: Boolean = true
  def withOpt(opt: OptLike): PartialCmd = copy(options = options :+ opt)
  def withError(error: ParseError): PartialCmd = copy(errors = errors :+ error)
}
object PartialCmd {
  lazy val empty = PartialCmd("")
}
final case class Cmd(name: String, options: Seq[OptLike], args: Seq[String] = Seq.empty) extends CmdLike {
  def isPartial: Boolean = options.exists(_.isPartial)
}
final case class Piped(commands: Seq[Cmd]) extends CmdLike {
  def isPartial: Boolean = commands.exists(_.isPartial)
  val name: String = ""
}

sealed trait OptLike extends Elem {
  val name: String
}
final case class PartialOpt(name: String, error: ParseError) extends OptLike {
  def isPartial: Boolean = true
}
object PartialOpt {
  def apply(name: String, error: => ParseError): PartialOpt = PartialOpt(name, error)
}
final case class Opt(name: String, value: OptValue, isPartial: Boolean) extends OptLike
object Opt {
  def apply(name: String): Opt = Opt(name, "")
  def apply(name: String, value: String, isPartial: Boolean): Opt = Opt(name, OptValue(value), isPartial)
  def apply(name: String, value: String): Opt = Opt(name, value, isPartial = false)
}
final case class OptValue(value: String)

final case class CmdSet(cmdDefs: Seq[CmdDef]) {
  def lift(name: String, args: Seq[String]): CmdLike = cmdDefs.find(_.name == name) match {
    case Some(cmdDef) => cmdDef.lift(args)
    case _ => PartialCmd(name).withError(UnknownCmdError(name))
  }
}
final case class CmdDef(name: String, opts: Seq[OptDef]) {
  def lift(inputs: Seq[String]): CmdLike = lift(inputs, Seq.empty, Seq.empty)
  def lift(inputs: Seq[String], options: Seq[OptLike], args: Seq[String]): CmdLike = if (inputs.isEmpty) {
    opts.foldLeft[Seq[ParseError]](options.collect { case PartialOpt(_, err) => err }) { (errors, optDef) =>
      if (optDef.require && !options.exists(_.name == optDef.name)) errors :+ RequiredOptError()
      else errors
    } match {
      case errors if errors.isEmpty => Cmd(name, options, args)
      case errors => PartialCmd(name, options, errors)
    }
  } else {
    opts.find(_.matching(inputs.head)) match {
      case Some(optDef) =>
        optDef.lift(inputs.head.replaceAll("^\\-+", ""), inputs.tail) match {
          case (o, t) => lift(t, options :+ o, args)
        }
      case _ => lift(inputs.tail, options, args :+ inputs.head)
    }
  }
}
object CmdDef {
  def apply(name: String): CmdDef = CmdDef(name, Seq.empty)
}
final case class OptDef(name: String, withParam: Boolean = false, require: Boolean = false) {
  def lift(head: String, tail: Seq[String]): (OptLike, Seq[String]) =
    if (withParam) {
      if (tail.isEmpty) (PartialOpt(name, ParamNotFoundError), tail)
      else (Opt(head, tail.head), tail.tail)
    } else (Opt(head), tail)
  def matching(str: String): Boolean = str.replaceAll("^\\-+", "") == name
}

