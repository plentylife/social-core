package life.plenty.model.utils

import life.plenty.model
import life.plenty.model.connection._
import life.plenty.model.octopi.{Contribution, Space, User}
import life.plenty.model.octopi.definition.Hub
import rx.{Ctx, Rx, Var}

import scala.language.postfixOps

object GraphUtils {
  def getParent(o: Hub)(implicit ctx: Ctx.Owner) = o.rx.get({ case Parent(p: Hub) ⇒ p })

  def getRootParentConnection(o: Hub)(implicit ctx: Ctx.Owner) = o.rx.get({ case c@RootParent(_) ⇒ c })

  def getRootParent(o: Hub)(implicit ctx: Ctx.Owner) = getRootParentConnection(o).map(_.map(_.value))

  def getRootParentOrSelf(o: Hub)(implicit ctx: Ctx.Owner): Rx.Dynamic[Hub] =
    getRootParentConnection(o).map(_.map(_.value).getOrElse(o))

  def confirmedMarker(o: Hub)(implicit ctx: Ctx.Owner): Rx[Option[Marker]] =
    o.rx.get({ case c@Marker(m) if m == MarkerEnum.CONFIRMED ⇒ c })

  def markedConfirmed(o: Hub)(implicit ctx: Ctx.Owner): Rx[Boolean] =
    confirmedMarker(o).map(m ⇒ {m.nonEmpty})

  def getBody(h: Hub)(implicit ctx: Ctx.Owner): Rx[Option[String]] = h.rx.get({ case Body(b) ⇒ b })

  // fixme use h.connections
  def isActive(o: Hub)(implicit ctx: Ctx.Owner): Rx[Boolean] = {
    val count: Rx[List[Int]] = o.connections.map {
      _ collect {
        case Inactive(_) ⇒ -1
        case Active(_) ⇒ 1
      }
    }
    count map { list: List[Int] ⇒
      val s = (0 :: list).sum
      s >= 0
    }
  }

  def getAllContributionsInSpace(space: Space, user: User)(implicit ctx: Ctx.Owner): Rx[List[Contribution]] = {
    def filterContributionByCreator(hub: DataHub[_]): Rx[Option[Contribution]] = hub match {
      case Child(hub: Contribution) ⇒
        println(s"processing contribution ${hub.getCreator}")
        hub.getCreator.flatMapRx(h ⇒ {if (h.id == user.id) Option(hub) else None})
      case _ ⇒ Rx {None}
    }

    collectDownTree[Contribution](space, matchBy = filterContributionByCreator,
      allowedPath = {case Child(h: Hub) ⇒ h}, 5000)
  }

  def findModuleUpParentTree[T](in: Hub, matchBy: PartialFunction[DataHub[_], T]): Option[T] = {
    {
      //      println(s"graph utils", in)
      val within = in.sc.ex(matchBy)
      //                  println("graph utils", within, in, in.connections)
      within orElse {
        in.sc.ex({ case Parent(p: Hub) ⇒ p }) flatMap {
          p ⇒
            if (p == in) {
              println("Error in findModule of ActionAddMember: same parent")
              None
            } else {
              findModuleUpParentTree(p, matchBy)
            }
        }
      }
    }
  }

  import rx.async._
  import rx.async.Platform._
  import scala.concurrent.duration._

  /** @param matchBy should be able to handle [[DataHub]] */
  def collectDownTree[T](in: Hub, matchBy: DataHub[_] ⇒ Rx[Option[T]],
                      allowedPath: PartialFunction[DataHub[_],Hub], debounceDuration: Int = 0)
                     (implicit ctx: Ctx.Owner): Rx[List[T]] = Rx {
    val pathCons = in.rx.getAll(allowedPath).debounce(debounceDuration millis)
    val hubs = in.rx.cons.debounce(debounceDuration millis) map {list ⇒
      println(s"looking for hubs in $list")
      list flatMap {h ⇒ val r = matchBy(h); r()}
    }

//    model.console.trace(s"collectDownTree ${pathCons} | $hubs")
    println(s"${in} -->\n\t collectDownTree $hubs || $pathCons ")

    val nextHubs = pathCons() flatMap { h ⇒
      val r = collectDownTree(h, matchBy, allowedPath)
      r()
    }

    hubs() ::: nextHubs
  }
}