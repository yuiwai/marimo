package com.yuiwai.marimo.player.api

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Json, OFormat}

trait PlayerService extends Service {
  def getStatus: ServiceCall[NotUsed, PlayerStatus]
  def create: ServiceCall[NotUsed, PlayerStatus]
  override def descriptor: Descriptor = {
    import Service._

    named("player")
      .withCalls(
        namedCall("create", create)
      )
      .withAutoAcl(true)
  }
}

case class PlayerStatus(life: Long)
object PlayerStatus {
  implicit val format: OFormat[PlayerStatus] = Json.format[PlayerStatus]
}
