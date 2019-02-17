package com.yuiwai.marimo.world

import akka.NotUsed
import com.lightbend.lagom.scaladsl.server.LocalServiceLocator
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, Matchers}
import com.lightbend.lagom.scaladsl.testkit.ServiceTest
import com.yuiwai.marimo.world.api.WorldService

class WorldServiceSpec extends AsyncWordSpec with Matchers with BeforeAndAfterAll {
  lazy val server = ServiceTest.startServer(ServiceTest.defaultSetup.withCassandra()) { ctx =>
    new WorldApplication(ctx) with LocalServiceLocator
  }
  lazy val client = server.serviceClient.implement[WorldService]

  "WorldService" should {
    "create new World" in {
      client.create.invoke(NotUsed).map { _ => 1 should ===(1) }
    }
    "register player" in {
      client.registerPlayer.invoke(NotUsed).map { response =>
        true should ===(true)
      }
    }
  }

  override protected def beforeAll() = server
  override protected def afterAll() = server.stop()
}
