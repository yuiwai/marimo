package com.yuiwai.marimo.core

import java.util.concurrent.atomic.AtomicInteger

import com.yuiwai.marimo.core.World.{PlayerStatus, Registered, RegistrationRequested, Rejected}

final case class World(fields: Map[FieldId, Field], players: Map[PlayerId, PlayerStatus])(implicit rule: GameRule) {
  require(fields.forall(f => f._1 == f._2.fieldId))
  def field(pos: Pos): Option[Field] = fields.get(FieldId(pos))
  def registerPlayer(playerId: PlayerId): Either[WorldError, (World, WorldEvent)] = {
    if (players.count(_._2 != Rejected) >= rule.playerLimit) Left(PlayerLimitReached)
    else Right(copy(players = players + (playerId -> RegistrationRequested)), PlayerRegistrationRequested(playerId))
  }
  def acceptedPlayer(playerId: PlayerId): Either[WorldError, (World, WorldEvent)] = players.get(playerId) match {
    case Some(s) =>
      if (s == RegistrationRequested)
        Right(copy(players = players.updated(playerId, Registered)) -> PlayerRegistered(playerId))
      else Left(PlayerAlreadyRegistered)
    case None => Left(PlayerNotFound)
  }
}
object World {
  sealed trait PlayerStatus
  case object RegistrationRequested extends PlayerStatus
  case object Registered extends PlayerStatus
  case object Rejected extends PlayerStatus
  def empty(implicit rule: GameRule): World = apply(Seq.empty)
  def apply(fields: Seq[Field])(implicit rule: GameRule): World =
    new World(fields.map(f => f.fieldId -> f).toMap, Map.empty)
  def apply(field: Field, fields: Field*)(implicit rule: GameRule): World = apply(field +: fields)
}
final case class WorldId(id: Int)
sealed trait WorldEvent
final case class PlayerRegistrationRequested(playerId: PlayerId) extends WorldEvent
final case class PlayerRegistered(playerId: PlayerId) extends WorldEvent

sealed trait WorldError extends WorldEvent
case object PlayerLimitReached extends WorldError
case object PlayerNotFound extends WorldError
case object PlayerAlreadyRegistered extends WorldError

trait GameRule {
  def playerLimit: Int
}
trait WithDefaultRule {
  implicit val rule: GameRule = DefaultRule
}
object DefaultRule extends GameRule {
  override def playerLimit: Int = 100
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
case object FieldObjectListQuery extends FieldQuery
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
final case class ItemId(id: Int)

final case class Treasure(items: Map[ItemId, Int])
final case class TreasureBox(treasures: Seq[Treasure])

final case class Player(playerId: PlayerId, inventory: Inventory, wallet: Wallet, life: Life)
  extends WithBattleStatus[Player] {
  def payment(bill: Bill): Either[PlayerError, Player] = wallet.paid(bill.price)
    .map { w =>
      copy(inventory = inventory.add(bill.itemId), wallet = w)
    }
  def modifiedLife(f: Life => Life): Player = copy(life = f(life))
}
object Player {
  def empty(id: Int): Player = apply(PlayerId(id), Inventory.empty, Wallet.empty, Life(100))
}
final case class PlayerId(id: Int)
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

final case class Market(marketId: MarketId, products: Map[ProductId, Product], wishList: Map[ItemId, Int]) {
  private val ai = new AtomicInteger(0)
  private def newId() = ProductId(ai.incrementAndGet())
  private def modified(productId: ProductId)(f: Product => Either[MarketError, Product]): Either[MarketError, Market] = product(productId) match {
    case Some(p) => f(p).map(r => copy(products = products.updated(productId, r)))
    case None => Left(ProductNotFound)
  }
  def product(productId: ProductId): Option[Product] = products.get(productId)
  def wanted(itemId: ItemId): Market = copy(wishList = wishList.updated(itemId, wishList.getOrElse(itemId, 0) + 1))
  def order(): OrderSlip = OrderSlip(wishList)
  def arrive(itemId: ItemId, price: Currency): Market = copy(
    products = products.updated(newId(), Product(itemId, price, OnSale)),
    wishList = wishList.updated(itemId, wishList.get(itemId).map(_ - 1).getOrElse(0))
  )
  def buy(productId: ProductId): Either[MarketError, Market] = modified(productId) {
    case p if p.onSale => Right(p.sold)
    case _ => Left(ProductAlreadySold)
  }
  def delivered(productId1: ProductId): Either[MarketError, Market] = product(productId1) match {
    case Some(p) if p.soldOut =>
      Right(copy(
        products = products - productId1,
        wishList = wishList.updated(p.itemId, wishList.getOrElse(p.itemId, 0) + 1))
      )
    case Some(p) if p.onSale => Left(ProductIsOnSale)
    case None => Left(ProductNotFound)
  }
}
object Market {
  def apply(marketId: MarketId): Market = Market(marketId, Map.empty, Map.empty)
  def empty(id: Int): Market = Market(MarketId(id))
}
final case class MarketId(id: Int)
sealed trait MarketEvent
final case class ProductPurchased(productId: ProductId /*, playerId: PlayerId */) extends MarketEvent
sealed trait MarketError extends MarketEvent
case object ProductNotFound extends MarketError
case object ProductAlreadySold extends MarketError
case object ProductIsOnSale extends MarketError
final case class Product(itemId: ItemId, price: Currency, state: ProductState) {
  def onSale: Boolean = state == OnSale
  def soldOut: Boolean = state == SoldOut
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
final case class OrderSlip(items: Map[ItemId, Int]) {
  def count(itemId: ItemId): Int = items.getOrElse(itemId, 0)
}
object Currency {
  def zero: Currency = Currency(0)
  def apply(value: Int): Currency = {
    require(value >= 0)
    new Currency(value)
  }
}
final case class Bill(itemId: ItemId, price: Currency)

final case class Monster(monsterId: MonsterId, life: Life) extends WithBattleStatus[Monster] {
  def modifiedLife(f: Life => Life): Monster = copy(life = f(life))
}
final case class MonsterId()

final case class Attack(damage: Int)
final case class Life(currentLife: Int, maxLife: Int) {
  require(currentLife >= 0)
  def -(damage: Int): Life = copy(currentLife = (currentLife - damage).max(0))
  def isDead: Boolean = !isAlive
  def isAlive: Boolean = currentLife > 0
}
object Life {
  def apply(life: Int): Life = apply(life, life)
}
trait WithBattleStatus[T] {
  val life: Life
  def isDead: Boolean = life.isDead
  def modifiedLife(f: Life => Life): T
  def damaged(attack: Attack): T = modifiedLife(_ - attack.damage)
  def currentLife: Int = life.currentLife
}

// TODO Services
trait Service
trait PlayerService extends Service
trait FieldService extends Service
trait ItemService extends Service
trait MarketService extends Service

trait ActivityStream
