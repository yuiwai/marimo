package controllers

import com.yuiwai.marimo.field.api.{FieldId, FieldService}
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.ExecutionContext

class Main(fieldService: FieldService, controllerComponents: ControllerComponents)
  (implicit ec: ExecutionContext) extends AbstractController(controllerComponents) {

  def index = Action { _ =>
    Ok(views.html.main())
  }

  def fieldObjects = Action.async { _ =>
    fieldService
      .getObjects
      .invoke(FieldId(0, 0))
      .map { objects =>
        Ok(s"fieldObjects: $objects")
      }
  }
}
