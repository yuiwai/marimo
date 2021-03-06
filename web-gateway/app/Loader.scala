package loader

import com.lightbend.lagom.scaladsl.api.{LagomConfigComponent, ServiceAcl, ServiceInfo}
import com.lightbend.lagom.scaladsl.client.LagomServiceClientComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.rp.servicediscovery.lagom.scaladsl.LagomServiceLocatorComponents
import com.softwaremill.macwire._
import com.yuiwai.marimo.field.api.FieldService
import controllers.{AssetsComponents, Main}
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, Mode}
import play.filters.HttpFiltersComponents
import router.Routes

import scala.collection.immutable
import scala.concurrent.ExecutionContext

abstract class WebGateway(context: ApplicationLoader.Context)
  extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents
  with AssetsComponents
  with AhcWSComponents
  with LagomConfigComponent
  with LagomServiceClientComponents {
  override lazy val serviceInfo = ServiceInfo(
    "web-gateway",
    Map(
      "web-gateway" -> immutable.Seq(ServiceAcl.forPathRegex("(?!/api/).*"))
    )
  )
  override lazy val router = {
    val prefix = "/"
    wire[Routes]
  }
  lazy val fieldService = serviceClient.implement[FieldService]
  override implicit lazy val executionContext: ExecutionContext = actorSystem.dispatcher
  lazy val main = wire[Main]
}

class WebGatewayLoader extends ApplicationLoader {
  override def load(context: Context) = context.environment.mode match {
    case Mode.Dev =>
      (new WebGateway(context) with LagomDevModeComponents).application
    case _ =>
      (new WebGateway(context) with LagomServiceLocatorComponents).application
  }
}
