package com.yuiwai.marimo.market

import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import com.yuiwai.marimo.market.api.MarketService
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}

class MarketServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {
  lazy val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra()) { ctx =>
    new MarketApplication(ctx) with LocalServiceLocator
  }
  lazy val client = server.serviceClient.implement[MarketService]

  "MarketService" should {
  }

  override protected def beforeAll() = server
  override protected def afterAll() = server.stop()
}
