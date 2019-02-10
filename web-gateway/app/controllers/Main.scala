package controllers

import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

class Main(controllerComponents: ControllerComponents)
  (implicit ec: ExecutionContext) extends AbstractController(controllerComponents) {

  def index = Action { _ =>
    Ok(views.html.main())
  }
}
