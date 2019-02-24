package com.yuiwai.marimo.js

import java.nio.ByteBuffer

import boopickle.Default._
import com.yuiwai.marimo.shared.FieldObject
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}

object Main extends ApiCall {
  def main(args: Array[String]): Unit = {
    val main = MainView.Component
    main(MainView.Props(runCommandHandler))
      .renderIntoDOM(
        dom.document.getElementById("react-stage")
      )
  }
  def runCommandHandler(str: String): Unit = {
    Commands.parse(str)
    // fieldObjects.foreach(println)
  }
}

trait ApiCall {
  def fieldObjects: Future[Seq[FieldObject]] =
    callApi("/fieldObjects")
      .map(Unpickle.apply[Seq[FieldObject]].fromBytes)
  private def callApi(path: String): Future[ByteBuffer] = Ajax
    .get(path, responseType = "arraybuffer")
    .map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))
}
