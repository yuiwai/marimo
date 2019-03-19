package com.yuiwai.marimo.market

import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import com.yuiwai.marimo.market.api.MarketService
import play.api.libs.ws.ahc.AhcWSComponents

import scala.collection.immutable

abstract class MarketApplication(ctx: LagomApplicationContext) extends LagomApplication(ctx)
  with AhcWSComponents
  with LagomServerComponents
  with CassandraPersistenceComponents {
  override def jsonSerializerRegistry: JsonSerializerRegistry = WorldSerializationRegistry
  override def lagomServer: LagomServer = serverFor[MarketService](wire[MarketServiceImpl])
}

class MarketApplicationLoader extends LagomApplicationLoader {
  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new MarketApplication(context) with LagomDevModeComponents
  override def load(context: LagomApplicationContext): LagomApplication = ???
}

object WorldSerializationRegistry extends JsonSerializerRegistry {
  override def serializers: immutable.Seq[JsonSerializer[_]] = immutable.Seq()
}
