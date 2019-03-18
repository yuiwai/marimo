package com.yuiwai.marimo.js

import com.yuiwai.marimo.cli.{Cmd, CmdDef, CmdSet}

object Commands {
  implicit val cmdSet: CmdSet = CmdSet(Seq(
    CmdDef("test"),
    CmdDef("echo")
  ))
  def parse(str: String): Unit = {
    Cmd.parse(str) match {
      case Right(cmd: Cmd) => run(cmd)
      case Left(parseError) => println(parseError)
      case other => println(other)
    }
  }
  def run(cmd: Cmd): Unit = {
    println(cmd)
  }
}
