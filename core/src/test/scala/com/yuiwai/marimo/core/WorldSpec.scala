package com.yuiwai.marimo.core

import utest._

object WorldSpec extends TestSuite {
  val tests = Tests {
    "register player" - {
      val playerId1 = PlayerId(1)
      val playerId2 = PlayerId(2)

      "without limit" - {
        implicit val rule: GameRule = DefaultRule
        val world = World(Seq.empty)
        world.registerPlayer(playerId1) match {
          case Right(w) =>
            w.players.size ==> 1
            w.players(playerId1) ==> true
            w.players(playerId2) ==> false
        }
        world
          .registerPlayer(playerId1)
          .flatMap(_.registerPlayer(playerId2)) match {
          case Right(w) =>
            w.players.size ==> 2
            w.players(playerId1) ==> true
            w.players(playerId2) ==> true
        }
      }

      "with limit" - {
        implicit val rule: GameRule = new GameRule {
          override def playerLimit: Int = 1
        }
        val world = World(Seq.empty)
        world
          .registerPlayer(playerId1)
          .flatMap(_.registerPlayer(playerId2)) ==> Left(PlayerLimitReached)
      }
    }
  }
}

object FieldSpec extends TestSuite {
  val tests = Tests {
    implicit val rule: GameRule = DefaultRule
    "left, right, up, down" - {
      val field1_1 = TownField(FieldId(Pos(1, 1)), Seq.empty)
      val field1_2 = WoodsField(FieldId(Pos(1, 2)), Seq.empty)
      val field2_2 = GrasslandField(FieldId(Pos(2, 2)), Seq.empty)
      implicit val world: World = World(Seq(field1_1, field1_2, field2_2))
      field2_2.left ==> Some(field1_2)
      field1_2.right ==> Some(field2_2)
      field1_2.up ==> Some(field1_1)
      field1_1.down ==> Some(field1_2)
    }
  }
}

object FieldQuerySpec extends TestSuite {
  val tests = Tests {
    "object list" - {
      // TownField()
    }
  }
}

object MarketSpec extends TestSuite {
  val tests = Tests {
    def gen(): Market = Market(MarketId(), Map.empty, Map.empty)
    def genProduct(): Product = Product(ItemId(1), Currency(100), OnSale)

    val productId1 = ProductId(1)
    "buy" - {
      val market = Market(MarketId(), Map(productId1 -> genProduct()), Map.empty)
      market.buy(productId1).right.get.product(productId1).get.state ==> SoldOut
    }
    "order" - {
      val itemId = ItemId(1)
      val market = gen()
      "without wish list" - {
        market.order().count(itemId) ==> 0
      }
      "with wish list" - {
        market.wanted(itemId).order().count(itemId) ==> 1
      }
    }
    "wanted" - {
      val itemId = ItemId(1)
      gen().wishList.getOrElse(itemId, 0) ==> 0
      gen().wanted(itemId).wishList.getOrElse(itemId, 0) ==> 1
      gen().wanted(itemId).wanted(itemId).wishList.getOrElse(itemId, 0) ==> 2
    }
    "arrive" - {
      val itemId = ItemId(1)
      val market = gen()
        .wanted(itemId)
        .arrive(itemId, Currency(10))
      market.products.size ==> 1
      market.wishList(itemId) ==> 0
    }
    "delivered" - {
      "with sold out" - {
        val market = Market(MarketId(), Map(productId1 -> genProduct().sold), Map.empty)
          .delivered(productId1).right.get
        market.products.size ==> 0
        market.wishList.size ==> 1
      }
      "with on sale" - {
        val market = Market(MarketId(), Map(productId1 -> genProduct()), Map.empty)
        market.delivered(productId1).left.get ==> ProductIsOnSale
      }
    }
  }
}

object PlayerSpec extends TestSuite {
  val tests = Tests {
    "payment" - {
      val playerId = PlayerId(1)
      "currency lacked" - {
        Player(playerId, Inventory.empty, Wallet.empty, Life(100))
          .payment(Bill(ItemId(1), Currency(100)))
          .left.get ==> CurrencyLacked
      }
      "purchased" - {
        Player(playerId, Inventory.empty, Wallet(Currency(100)), Life(100))
          .payment(Bill(ItemId(1), Currency(100)))
          .right.get.inventory.size ==> 1
      }
    }
  }
}

object BattleSpec extends TestSuite {
  val tests = Tests {
    "player attacks monster" - {
      Monster(MonsterId(), Life(100))
        .damaged(Attack(51))
        .currentLife ==> 49
    }
    "monster attacks player" - {
      Player(PlayerId(1), Inventory.empty, Wallet.empty, Life(100))
        .damaged(Attack(51))
        .currentLife ==> 49
    }
    "death of monster" - {
      Monster(MonsterId(), Life(100))
        .damaged(Attack(100))
        .isDead ==> true
    }
  }
}
