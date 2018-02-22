package life.plenty.model.octopi.definition

import life.plenty.model.connection.DataHub
import life.plenty.model.modifiers.RxConnectionFilters
import rx.{Ctx, Rx, Var}

trait RxConnectionManager {
  self: Hub ⇒
  protected lazy val _connectionsRx: Var[List[Rx[Option[DataHub[_]]]]] = Var(List.empty[Rx[Option[DataHub[_]]]])

  private lazy val connectionFilters = getAllModules({ case m: RxConnectionFilters[_] ⇒ m })

  private var onConnectionsRequest: List[() ⇒ Unit] = List()

  def addOnConnectionRequestFunctions(fList: List[() ⇒ Unit]): Unit = onConnectionsRequest :::= fList

  onConnectionAddedOperation(connection ⇒ {
    /*filtering block*/
    val filteredCon: Rx[Option[DataHub[_]]] = connectionFilters.foldLeft[Rx[Option[DataHub[_]]]](
      Var {Option(connection)}
    )((c, f) ⇒ f(c))
    _connectionsRx() = filteredCon :: (_connectionsRx.now: List[Rx[Option[DataHub[_]]]])
    /* end block */
  })

  object rx {
    type RxConsList = Rx[List[DataHub[_]]]

    def toRxConsList(in: Var[scala.List[Rx[Option[DataHub[_]]]]])(implicit ctx: Ctx.Owner): RxConsList = {
      in.map({ list ⇒
        list.flatMap(rx ⇒ {rx()})
      })
    }

    def cons(implicit ctx: Ctx.Owner): RxConsList = {
      onConnectionsRequest.foreach(f ⇒ f())
      toRxConsList(_connectionsRx)
    }

    /** @return first found non empty rx */
    def get[T](f: PartialFunction[DataHub[_], T])(implicit ctx: Ctx.Owner): Rx[Option[T]] = {
      onConnectionsRequest.foreach(f ⇒ f())

      Rx {
        var res:Rx[Option[T]] = Var[Option[T]](None)
        val obs = _connectionsRx.foreach(listOfRx ⇒ {
          val rightRx = listOfRx.find(rx ⇒ rx().collect(f).nonEmpty)
          rightRx foreach {rx ⇒
            res = rx map {_.collect(f)}
          }
        })
        if (res().nonEmpty) obs.kill()
        res()
      }
    }
    
//    def get[T](f: PartialFunction[DataHub[_], T])(implicit ctx: Ctx.Owner): Rx[Option[T]] = {
//      val res = cons(ctx) map {_.collectFirst(f)}
//    }

    def getAll[T](f: PartialFunction[DataHub[_], T])(implicit ctx: Ctx.Owner): Rx[List[T]] = {
      onConnectionsRequest.foreach(f ⇒ f())
      Lazy.getAll(f)
    }

    object Lazy {
      def lazyCons(implicit ctx: Ctx.Owner): RxConsList = toRxConsList(_connectionsRx)

      def lazyGet[T](f: PartialFunction[DataHub[_], T])(implicit ctx: Ctx.Owner): Rx[Option[T]] =
        lazyCons(ctx) map {_.collectFirst(f)}

      def getAll[T](f: PartialFunction[DataHub[_], T])(implicit ctx: Ctx.Owner): Rx[List[T]] = {
        // todo. this can be optimized with getWatch
        _connectionsRx.map(_ map { rx ⇒
          rx.map({ opt ⇒ opt.collect(f) })(ctx)
        } flatMap { rx ⇒ rx() })(ctx)
      }
    }

  }

}
