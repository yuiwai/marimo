package com.yuiwai.marimo.world

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.yuiwai.marimo.world.api.{WorldInfo, WorldService}

import scala.concurrent.{ExecutionContext, Future}

class WorldServiceImpl(implicit ec: ExecutionContext) extends WorldService {
  override def create: ServiceCall[NotUsed, WorldInfo] = ServiceCall { _ =>
    Future(WorldInfo(0))
  }
  override def registerPlayer: ServiceCall[NotUsed, Long] = ServiceCall { _ =>
    Future(0)
  }
}
