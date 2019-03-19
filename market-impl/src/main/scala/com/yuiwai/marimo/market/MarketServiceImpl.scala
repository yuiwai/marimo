package com.yuiwai.marimo.market

import akka.Done
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.yuiwai.marimo.market.api.{MarketService, PurchaseRequest}

class MarketServiceImpl extends MarketService {
  override def purchase: ServiceCall[PurchaseRequest, Done] = ???
}
