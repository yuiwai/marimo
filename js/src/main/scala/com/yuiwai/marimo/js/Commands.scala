package com.yuiwai.marimo.js

import com.yuiwai.marimo.cli.{Cmd, CmdSet, Piped}

object Commands {
  implicit val cmdSet: CmdSet = CmdSet(Seq.empty)
  def parse(str: String): Unit = {
    Cmd.parse(str) match {
      case Right(Piped(commands)) => println(commands)
      case Left(parseError) => println(parseError)
    }
  }
}
