package com.yuiwai.marimo.js

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.ext.KeyCode

object MainView {
  type RunCommandHandler = String => Unit
  final case class Props(
    runCommandHandler: RunCommandHandler
  ) {
    @inline def render: VdomElement = Component(this)
  }
  final case class State(command: String)
  object State {
    def init: State = State("test")
  }
  final class Backend(bs: BackendScope[Props, State]) {
    def render(s: State, p: Props): VdomElement =
      <.div(
        <.input.text(
          ^.value := s.command,
          ^.onChange ==> ((e: ReactEventFromInput) => {
            e.persist()
            bs.modState(_.copy(command = e.target.value))
          }),
          ^.onKeyUp ==> ((e: ReactKeyboardEvent) => Callback(e.keyCode match {
            case KeyCode.Enter => p.runCommandHandler(s.command)
            case _ => ()
          }))
        )
      )
  }

  val Component = ScalaComponent
    .builder[Props]("MainView")
    .initialState(State.init)
    .renderBackend[Backend]
    .build
}
