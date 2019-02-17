package com.yuiwai.marimo.world

import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import com.yuiwai.marimo.world.api.WorldService
import play.api.libs.ws.ahc.AhcWSComponents

abstract class WorldApplication(context: LagomApplicationContext) extends LagomApplication(context)
  with AhcWSComponents
  with LagomServerComponents {
  override def lagomServer: LagomServer = serverFor[WorldService](wire[WorldServiceImpl])
}
class WorldApplicationLoader extends LagomApplicationLoader {
  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new WorldApplication(context) with LagomDevModeComponents
  override def load(context: LagomApplicationContext): LagomApplication = ???
}
