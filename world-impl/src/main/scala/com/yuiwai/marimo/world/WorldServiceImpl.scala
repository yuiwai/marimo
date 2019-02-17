package com.yuiwai.marimo.world

import java.util.UUID

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import com.yuiwai.marimo.world.api.{WorldInfo, WorldService}

import scala.concurrent.{ExecutionContext, Future}

class WorldServiceImpl(entityRegistry: PersistentEntityRegistry)(implicit ec: ExecutionContext) extends WorldService {
  entityRegistry.register(new WorldEntity)
  override def create: ServiceCall[NotUsed, WorldInfo] = ServiceCall { _ =>
    val entityId = UUID.randomUUID().toString
    val ref = entityRegistry.refFor[WorldEntity](entityId)
    ref
      .ask(Create())
      .map(_ => WorldInfo(entityId))
  }
  override def registerPlayer: ServiceCall[NotUsed, Long] = ServiceCall { _ =>
    Future(0)
  }
}
