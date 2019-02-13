package com.yuiwai.marimo.field

import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import com.yuiwai.marimo.field.api.FieldService
import play.api.libs.ws.ahc.AhcWSComponents

abstract class FieldApplication(context: LagomApplicationContext) extends LagomApplication(context)
  with AhcWSComponents
  with LagomServerComponents {
  override def lagomServer: LagomServer = serverFor[FieldService](wire[FieldServiceImpl])
}

class FieldApplicationLoader extends LagomApplicationLoader {
  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new FieldApplication(context) with LagomDevModeComponents
  override def load(context: LagomApplicationContext): LagomApplication = ???
}
