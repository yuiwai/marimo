package com.yuiwai.marimo.js

import org.scalajs.dom.ext.Ajax
import scala.concurrent.ExecutionContext.Implicits.global

object Main {
  def main(args: Array[String]): Unit = {
    Ajax
      .get("/fieldObjects")
      .foreach { r =>
        println(r)
      }
  }
}
