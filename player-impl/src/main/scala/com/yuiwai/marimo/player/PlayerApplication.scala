package com.yuiwai.marimo.player

import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import com.yuiwai.marimo.player.api.PlayerService
import play.api.libs.ws.ahc.AhcWSComponents

abstract class PlayerApplication(context: LagomApplicationContext) extends LagomApplication(context)
    with AhcWSComponents
    with LagomServerComponents {
  override def lagomServer: LagomServer = serverFor[PlayerService](wire[PlayerServiceImpl])
}

class PlayerApplicationLoader extends LagomApplicationLoader {
  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new PlayerApplication(context) with LagomDevModeComponents
  override def load(context: LagomApplicationContext): LagomApplication = ???
}
