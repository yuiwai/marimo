package com.yuiwai.marimo.core

import java.util.concurrent.atomic.AtomicInteger

final case class World(fields: Map[FieldId, Field]) {
  require(fields.forall(f => f._1 == f._2.fieldId))
  def field(pos: Pos): Option[Field] = fields.get(FieldId(pos))
}
object World {
  def apply(fields: Seq[Field]): World = new World(fields.map(f => f.fieldId -> f).toMap)
  def apply(field: Field, fields: Field*): World = apply(field +: fields)
}

final case class Pos(x: Int, y: Int) {
  require(x >= 0 && y >= 0)
  def left: Option[Pos] = if (x > 0) Some(copy(x = x - 1)) else None
  def right: Option[Pos] = if (x < Int.MaxValue) Some(copy(x = x + 1)) else None
  def up: Option[Pos] = if (y > 0) Some(copy(y = y - 1)) else None
  def down: Option[Pos] = if (y < Int.MaxValue) Some(copy(y = y + 1)) else None
}

sealed trait Field {
  val fieldId: FieldId
  def objects: Seq[FieldObject] = Seq.empty
  def pos: Pos = fieldId.pos
}
object Field {
  implicit class FieldWrap(field: Field) {
    import field._
    def left(implicit world: World): Option[Field] = pos.left.flatMap(world.field)
    def right(implicit world: World): Option[Field] = pos.right.flatMap(world.field)
    def up(implicit world: World): Option[Field] = pos.up.flatMap(world.field)
    def down(implicit world: World): Option[Field] = pos.down.flatMap(world.field)
  }
}
sealed trait WithMonster extends Field {
  override def objects: Seq[FieldObject] = super.objects ++ monsters
  def monsters: Seq[MonsterObject]
}
sealed trait WithTreasureBox {
  def treasureBoxes: Seq[TreasureBoxObject]
}
sealed trait WithMarket extends Field {
  override def objects: Seq[FieldObject] = super.objects ++ markets
  def markets: Seq[MarketObject]
}
sealed trait WithItem {
  def items: Seq[ItemObject]
}
final case class TownField(fieldId: FieldId, markets: Seq[MarketObject]) extends Field with WithMarket
final case class WoodsField(fieldId: FieldId, monsters: Seq[MonsterObject]) extends Field with WithMonster
final case class GrasslandField(fieldId: FieldId, monsters: Seq[MonsterObject]) extends Field with WithMonster
final case class MountainField(fieldId: FieldId, monsters: Seq[MonsterObject]) extends Field with WithMonster
final case class FieldId(pos: Pos)

sealed trait FieldObject {
  val fieldObjectId: FieldObjectId[_]
}
final case class MonsterObject(fieldObjectId: FieldObjectId[MonsterObject]) extends FieldObject
final case class PlayerObject(fieldObjectId: FieldObjectId[PlayerObject]) extends FieldObject
final case class MarketObject(fieldObjectId: FieldObjectId[MarketObject]) extends FieldObject
final case class ItemObject(fieldObjectId: FieldObjectId[ItemObject]) extends FieldObject
final case class TreasureBoxObject(fieldObjectId: FieldObjectId[TreasureBoxObject]) extends FieldObject
final case class FieldObjectId[T <: FieldObject]()
sealed trait FieldObjectType
case object TypeMonster extends FieldObjectType
case object TypePlayer extends FieldObjectType

sealed trait Command
sealed trait FieldCommand extends Command
final case class AttackCommand(target: AttackTarget)
final case class SkillCommand(target: SkillTarget)
final case class BuyCommand()
final case class SellCommand()
final case class UseItemCommand()
final case class MoveToCommand(to: FieldId)

sealed trait Query
sealed trait FieldQuery extends Query
case object ProductListQuery extends FieldQuery

sealed trait Event
sealed trait FieldEvent extends Event {
  val fieldId: FieldId
}
final case class FieldExited(fieldId: FieldId) extends FieldEvent
final case class FieldEntered(fieldId: FieldId) extends FieldEvent

sealed trait AttackTarget
case class AttackTargetSingleMonster(fieldObjectId: FieldObjectId[MonsterObject]) extends AttackTarget
case class AttackTargetSinglePlayer(fieldObjectId: FieldObjectId[PlayerObject]) extends AttackTarget
case object AttackTargetAll extends AttackTarget

sealed trait SkillTarget

sealed trait Item {
  val itemId: ItemId
}
sealed trait UsableItem extends Item
sealed trait EquipableItem extends Item
final case class Potion(itemId: ItemId) extends UsableItem
final case class ItemId()

final case class Treasure(items: Map[ItemId, Int])
final case class TreasureBox(treasures: Seq[Treasure])


final case class Player(playerId: PlayerId, inventory: Inventory, wallet: Wallet) {
  def payment(bill: Bill): Either[PlayerError, Player] = wallet.paid(bill.price)
    .map { w =>
      copy(inventory = inventory.add(bill.itemId), wallet = w)
    }
}
final case class PlayerId()
sealed trait PlayerError
case object CurrencyLacked extends PlayerError
final case class Inventory(items: Set[ItemId]) {
  def add(itemId: ItemId): Inventory = {
    // TODO check item count limit
    copy(items + itemId)
  }
  def size: Int = items.size
}
object Inventory {
  def empty: Inventory = Inventory(Set.empty)
}
final case class Wallet(currency: Currency) {
  def paid(price: Currency): Either[PlayerError, Wallet] =
    if (currency >= price) Right(copy(currency - price)) else Left(CurrencyLacked)
}
object Wallet {
  def empty: Wallet = Wallet(Currency.zero)
}

final case class Market(marketId: MarketId, products: Map[ProductId, Product]) {
  private val ai = new AtomicInteger(0)
  private def newId() = ProductId(ai.incrementAndGet())
  private def modified(productId: ProductId)(f: Product => Either[MarketError, Product]): Either[MarketError, Market] = get(productId) match {
    case Some(p) => f(p).map(r => copy(products = products.updated(productId, r)))
    case None => Left(ProductNotFound)
  }
  def get(productId: ProductId): Option[Product] = products.get(productId)
  def arrive(itemId: ItemId, price: Currency): Market = copy(
    products = products.updated(newId(), Product(itemId, price, OnSale))
  )
  def buy(productId: ProductId): Either[MarketError, Market] = modified(productId) {
    case p if p.onSale => Right(p.sold)
    case _ => Left(ProductAlreadySold)
  }
}
final case class MarketId()
sealed trait MarketError
case object ProductNotFound extends MarketError
case object ProductAlreadySold extends MarketError
final case class Product(itemId: ItemId, price: Currency, state: ProductState) {
  def onSale: Boolean = state == OnSale
  def sold: Product = copy(state = SoldOut)
}
sealed trait ProductState
case object OnSale extends ProductState
case object SoldOut extends ProductState
final case class ProductId(id: Int)
final case class Currency private(value: Int) extends AnyVal {
  def -(that: Currency): Currency = Currency(value - that.value)
  def +(that: Currency): Currency = Currency(value + that.value)
  def >(that: Currency): Boolean = value > that.value
  def <(that: Currency): Boolean = value < that.value
  def >=(that: Currency): Boolean = value >= that.value
  def <=(that: Currency): Boolean = value <= that.value
}
object Currency {
  def zero: Currency = Currency(0)
  def apply(value: Int): Currency = {
    require(value >= 0)
    new Currency(value)
  }
}
final case class Bill(itemId: ItemId, price: Currency)

final case class Monster(monsterId: MonsterId, life: Life) {
  def currentLife: Int = life.currentLife
  def damaged(attack: Attack): Monster = copy(life = life - attack.damage)
}
final case class MonsterId()

final case class Attack(damage: Int)
final case class Life(currentLife: Int, maxLife: Int) {
  require(currentLife >= 0)
  def -(damage: Int): Life = copy(currentLife = (currentLife - damage).max(0))
}
object Life {
  def apply(life: Int): Life = apply(life, life)
}


trait ActivityStream

