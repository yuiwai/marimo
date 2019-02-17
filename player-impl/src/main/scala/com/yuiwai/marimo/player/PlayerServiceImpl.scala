package com.yuiwai.marimo.player

import akka.NotUsed
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.yuiwai.marimo.player.api.{PlayerService, PlayerStatus}

import scala.concurrent.{ExecutionContext, Future}

class PlayerServiceImpl(implicit ec: ExecutionContext) extends PlayerService {
  override def getStatus: ServiceCall[NotUsed, PlayerStatus] = ServiceCall { _ =>
    Future(PlayerStatus(0))
  }
  override def create: ServiceCall[NotUsed, PlayerStatus] = ServiceCall { _ =>
    Future(PlayerStatus(0))
  }
}
