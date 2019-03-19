package com.yuiwai.marimo.market.api

import akka.Done
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import play.api.libs.json.{Json, OFormat}

trait MarketService extends Service {
  def purchase: ServiceCall[PurchaseRequest, Done]
  override def descriptor: Descriptor = {
    import Service._

    named("market")
      .withAutoAcl(true)
      .withCalls(
        call(purchase)
      )
  }
}

final case class PurchaseRequest(productId: Int)
object PurchaseRequest {
  implicit val format: OFormat[PurchaseRequest] = Json.format[PurchaseRequest]
}

