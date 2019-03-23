package com.yuiwai.marimo.demo

import com.yuiwai.marimo.core._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.higherKinds

object Main extends AnyRef
  with WithDefaultRule {
  val worldId = WorldId(1)
  val player1 = Player(PlayerId(1), Inventory.empty, Wallet.empty, Life(100))
  val marketId1 = MarketId(1)
  val productId1 = ProductId(1)
  def main(args: Array[String]): Unit = {
    worldScenario()
    purchaseScenario()
  }
  def worldScenario(): Unit = {
    WorldBehavior.registerPlayer(worldId, player1.playerId).foreach(println)
  }
  def purchaseScenario(): Unit = {
    MarketBehavior.purchase(marketId1, productId1).foreach(println)
    MarketBehavior.purchase2(marketId1, productId1).foreach(println)
  }
}

trait Behavior[State] {
  protected def resolve[F[_], Id](id: Id)
    (implicit resolver: Resolver[F, Id, State]): F[Option[State]] = resolver.resolve(id)
  protected def act[Command](state: State, command: Command)
    (implicit action: Action[State, Command]): (State, action.Event) = action(state, command)
  protected def applyEvent[Event](state: State, event: Event)
    (implicit applyEvent: ApplyEvent[State, Event]): State = applyEvent(state, event)
  protected def reply(state: State)
    (implicit presenter: Presenter[State]): presenter.Reply = presenter(state)
}
trait ActWithResolve[State] {
  self: Behavior[State] =>
  protected def actWithResolve[F[_] : Functor, Id, Command](id: Id, command: Command)
    (implicit
      resolver: Resolver[F, Id, State],
      action: Action[State, Command]
    ): F[Option[(State, Action[State, Command]#Event)]] = {
    implicitly[Functor[F]].map[Option[State], Option[(State, Action[State, Command]#Event)]](resolve(id), _.map(s => act(s, command)))
  }
}
trait Functor[F[_]] {
  def map[A, B](fa: F[A], f: A => B): F[B]
}

trait WorldBehavior extends Behavior[World] {
  trait WorldAction[Command <: WorldCommand] extends Action[World, Command] {
    override type Event = WorldEvent
  }
  implicit val registerPlayerAction = new WorldAction[RegisterPlayerCommand] {
    override def apply(world: World, command: RegisterPlayerCommand): (World, WorldEvent) = {
      world.registerPlayer(command.playerId) match {
        case Right(r) => r
        case Left(e) => world -> e
      }
    }
  }
  implicit val resolver = new Resolver[Future, WorldId, World] {
    override def resolve(id: WorldId): Future[Option[World]] = Future(Some(World.empty(DefaultRule)))
  }
  def registerPlayer(worldId: WorldId, playerId: PlayerId): Future[Option[(World, WorldEvent)]] = {
    resolve(worldId)
      .map(_.map(world => act(world, RegisterPlayerCommand(playerId))))
  }
}
object WorldBehavior extends WorldBehavior

sealed trait WorldCommand
final case class RegisterPlayerCommand(playerId: PlayerId) extends WorldCommand

trait MarketBehavior extends Behavior[Market] with ActWithResolve[Market] {
  implicit val marketResolver = new Resolver[Future, MarketId, Market] {
    override def resolve(id: MarketId): Future[Option[Market]] = Future(Some(Market(id)))
  }
  implicit val marketToString = new Presenter[Market] {
    override type Reply = String
    override def apply(state: Market): String = state.marketId.toString
  }
  implicit val purchaseAction = new Action[Market, PurchaseCommand] {
    override type Event = MarketEvent
    override def apply(market: Market, command: PurchaseCommand): (Market, MarketEvent) = {
      market.buy(command.productId) match {
        case Right(m) => m -> ProductPurchased(command.productId)
        case Left(e) => market -> e
      }
    }
  }
  implicit val future = new Functor[Future] {
    override def map[A, B](fa: Future[A], f: A => B): Future[B] = fa.map(f)
  }
  def productList(marketId: MarketId): Future[Option[String]] = resolve(marketId).map(_.map(reply(_)))
  def purchase(marketId: MarketId, productId: ProductId): Future[Option[(Market, MarketEvent)]] = {
    resolve(marketId).map(_.map(market => act(market, PurchaseCommand(productId))))
  }
  def purchase2(marketId: MarketId, productId: ProductId): Future[Option[(Market, Action[Market, PurchaseCommand]#Event)]] = {
    actWithResolve(marketId, PurchaseCommand(productId))
  }
}
object MarketBehavior extends MarketBehavior

sealed trait MarketCommand
final case class PurchaseCommand(productId: ProductId) extends MarketCommand

trait Action[State, Command] {
  type Event
  def apply(state: State, command: Command): (State, Event)
}
trait ApplyEvent[State, Event] {
  def apply(state: State, event: Event): State
}
trait Presenter[State] {
  type Reply
  def apply(state: State): Reply
}

trait Resolver[F[_], Id, State] {
  def resolve(id: Id): F[Option[State]]
}

trait Repository[F[_], Id, State] {
  type Reply
  def persist(id: Id, state: State): F[Reply]
}