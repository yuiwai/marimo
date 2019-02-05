package com.yuiwai.marimo.core

import utest._

object FieldSpec extends TestSuite {
  val tests = Tests {
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
      TownField()
    }
  }
}

object MarketSpec extends TestSuite {
  val tests = Tests {
    "buy" - {
      val productId1 = ProductId(1)
      val market = Market(MarketId(), Map(productId1 -> Product(ItemId(), Currency(100), OnSale)))
      market.buy(productId1).right.get.get(productId1).get.state ==> SoldOut
    }
    "arrive" - {
      val market = Market(MarketId(), Map.empty)
      market.arrive(ItemId(), Currency(10)).products.size ==> 1
    }
  }
}

object PlayerSpec extends TestSuite {
  val tests = Tests {
    "payment" - {
      "currency lacked" - {
        Player(PlayerId(), Inventory.empty, Wallet.empty, Life(100))
          .payment(Bill(ItemId(), Currency(100)))
          .left.get ==> CurrencyLacked
      }
      "purchased" - {
        Player(PlayerId(), Inventory.empty, Wallet(Currency(100)), Life(100))
          .payment(Bill(ItemId(), Currency(100)))
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
      Player(PlayerId(), Inventory.empty, Wallet.empty, Life(100))
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
