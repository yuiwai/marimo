package com.yuiwai.marimo.demo

import com.yuiwai.marimo.core._

object Main extends AnyRef
  with WithDefaultRule {
  def main(args: Array[String]): Unit = {
    val world = World(TownField(FieldId(Pos(1, 1)), Seq(MarketObject(FieldObjectId()))))
    val player1 = Player(PlayerId(1), Inventory.empty, Wallet.empty, Life(100))

    world.registerPlayer(player1.playerId)
  }
}
