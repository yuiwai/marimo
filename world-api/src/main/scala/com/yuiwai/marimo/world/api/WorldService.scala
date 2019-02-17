package com.yuiwai.marimo.world.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Json, OFormat}

trait WorldService extends Service {
  def create: ServiceCall[NotUsed, WorldInfo]
  def registerPlayer: ServiceCall[NotUsed, Long]
  override def descriptor: Descriptor = {
    import Service._

    named("world")
      .withCalls(
        namedCall("create", create),
        namedCall("registerPlayer", registerPlayer)
      )
      .withAutoAcl(true)
  }
}

final case class WorldInfo(worldId: String)
object WorldInfo {
  implicit val format: OFormat[WorldInfo] = Json.format[WorldInfo]
}
