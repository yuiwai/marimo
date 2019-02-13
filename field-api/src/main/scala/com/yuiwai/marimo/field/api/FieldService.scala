package com.yuiwai.marimo.field.api

import com.lightbend.lagom.scaladsl.api._
import play.api.libs.json.{Json, OFormat}

trait FieldService extends Service {
  def getObjects: ServiceCall[FieldId, Seq[FieldObject]]
  final override def descriptor: Descriptor = {
    import Service._

    named("field").withCalls(
      call(getObjects)
    )
      .withAutoAcl(true)
  }
}

final case class FieldId(x: Int, y: Int)
object FieldId {
  implicit val format: OFormat[FieldId] = Json.format[FieldId]
}
final case class FieldObject(objectType: Int)
object FieldObject {
  implicit val format: OFormat[FieldObject] = Json.format[FieldObject]
}