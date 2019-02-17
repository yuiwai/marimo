package com.yuiwai.marimo.world

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType

class WorldEntity extends PersistentEntity {
  override type Command = WorldCommand
  override type Event = WorldEvent
  override type State = WorldState
  override def initialState: WorldState = WorldState()
  override def behavior: Behavior = Actions()
    .onCommand[RegisterPlayer, Done] {
    case (_, ctx, _) =>
      ctx.thenPersist(PlayerRegistered()) { ev =>
        ctx.reply(Done)
      }
  }
}

sealed trait WorldCommand
final case class RegisterPlayer() extends WorldCommand with ReplyType[Done]

sealed trait WorldEvent
final case class PlayerRegistered() extends WorldEvent

final case class WorldState()
