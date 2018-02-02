package life.plenty.model.octopi

import life.plenty.model.actions._
import life.plenty.model.connection.MarkerEnum.MarkerEnum
import life.plenty.model.connection.{Connection, Marker}
import life.plenty.model.modifiers.{ModuleFilters, RxConnectionFilters}
import life.plenty.model.{ModuleRegistry, console}
import rx.{Ctx, Rx, Var}
trait Octopus extends OctopusConstructor {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  protected var _modules: List[Module[Octopus]] = List()
  protected lazy val _lastAddedConnection: Var[Rx[Option[Connection[_]]]] = Var(Var(None))
  protected lazy val _connections: Var[List[Connection[_]]] = Var(List.empty[Connection[_]])
  protected lazy val _connectionsRx: Var[List[Rx[Option[Connection[_]]]]] = Var(List.empty[Rx[Option[Connection[_]]]])

  //protected lazy val _connections: Rx.Dynamic[List[Connection[_]]] =
  //    _lastAddedConnection.fold(List.empty[Connection[_]])(
  //      (list: List[Connection[_]], elem: Option[Connection[_]]) ⇒ {elem.map(_ :: list).getOrElse(list)})

  private lazy val moduleFilters = getAllModules({ case m: ModuleFilters[_] ⇒ m })

  def modules: List[Module[Octopus]] = {
    console.trace(s"trying to get modules ${_modules}")
    console.trace(s"trying to get modules ${_modules} filters ${moduleFilters}")
    moduleFilters.foldLeft(_modules)((ms, f) ⇒ {
      f(ms)
    })
  }

  /** these modules are filtered */
  def getModules[T <: Module[Octopus]](matchBy: PartialFunction[Module[Octopus], T]): List[T] =
    modules.collect(matchBy)

  /** these modules do not have any filters applied */
  def getAllModules[T <: Module[Octopus]](matchBy: PartialFunction[Module[Octopus], T]): List[T] = {
    _modules.collect(matchBy)
  }

  def getTopModule[T <: Module[Octopus]](matchBy: PartialFunction[Module[Octopus], T]): Option[T] = {
    modules.collectFirst(matchBy)
  }

  def addModule(module: Module[Octopus]): Unit = {
    _modules = module :: _modules
    module match {
      case m: ActionOnAddToModuleStack[_] ⇒ m.onAddToStack()
      case _ ⇒
    }
  }

  /* Connections */
  private lazy val connectionFilters = getAllModules({ case m: RxConnectionFilters[_] ⇒ m })

  /** filters are no longer applied */
  @deprecated("This is not reliable due to the nature of gun loading")
  def connections: List[Connection[_]] = {
    allConnections
    //    connectionFilters.foldLeft(_connections.now)((cs, f) ⇒ f(cs))
  }

  /** no filters applied */
  def allConnections: List[Connection[_]] = _connections.now

  @deprecated("This is not reliable due to the nature of gun loading")
  def getTopConnection[T](f: PartialFunction[Connection[_], Connection[T]]): Option[Connection[T]] =
    connections.collectFirst(f)

  @deprecated("This is not reliable due to the nature of gun loading")
  def getTopConnectionData[T](f: PartialFunction[Connection[_], T]): Option[T] =
    connections.collectFirst(f)

  object s {
    def get[T](f: PartialFunction[Connection[_], Connection[T]]): Option[Connection[T]] = getTopConnection(f)

    def ex[T](f: PartialFunction[Connection[_], T]): Option[T] = getTopConnectionData(f)

    def exf[T](f: PartialFunction[Connection[_], T]): T = ex(f).get
  }

  object rx {
    type RxConsList = Rx[List[Connection[_]]]

    def cons(implicit ctx: Ctx.Owner): RxConsList = {
      console.trace(s"rx.cons ${onConnectionsRequestedModules} ${_connections}")
      onConnectionsRequestedModules.foreach(_.onConnectionsRequest())
      //      _connections

      _connectionsRx.map(_.flatMap(rx ⇒ rx()))
    }

    def get[T](f: PartialFunction[Connection[_], T])(implicit ctx: Ctx.Owner): Rx[Option[T]] =
      cons.now.collectFirst(f) match {
        case Some(c) ⇒ Var(Option(c))
        case None ⇒ getWatchOnce(f)(ctx)
      }

    def getAll[T](f: PartialFunction[Connection[_], T])(implicit ctx: Ctx.Owner): Rx[List[T]] = {
      val current = Var(cons.now.collect(f))
      // this is needed because otherwise the last element is included
      var firstWatch = current.now.nonEmpty
      getWatch(f)(ctx).foreach(_.foreach(n ⇒ if (!firstWatch) {
        current() = n :: current.now
      } else firstWatch = false))(ctx)
      current
    }

    // todo make it kill itself
    def getWatchOnce[T](f: PartialFunction[Connection[_], T])(implicit ctx: Ctx.Owner): Rx[Option[T]] = {
      _lastAddedConnection.map(rx ⇒ rx().collect(f))(ctx).filter(_.nonEmpty)(ctx)
    }

    def getWatch[T](f: PartialFunction[Connection[_], T])(implicit ctx: Ctx.Owner): Rx[Option[T]] = {
      _lastAddedConnection.map(rx ⇒ rx().collect(f))(ctx).filter(_.nonEmpty)(ctx)
    }

    //    def getAll[T <: Connection[_]](implicit ctx: Ctx.Owner): Rx[Iterable[T]] = _connections.
  }

  def hasMarker(marker: MarkerEnum): Boolean = connections.collect { case Marker(m) if m == marker ⇒ true } contains true

  def addConnection(connection: Connection[_]): Either[Exception, Unit] = {
    // duplicates are silently dropped
    //    println(s"adding connection ${connection} to ${this}")
    if (_connections.now.exists(_.id == connection.id)) {
      console.trace(s"Connection was not added since it exists ${connection}")
      return Right()
    }

    var onErrorList = Stream(getModules({ case m: ActionOnGraphTransform ⇒ m }): _*) map { m ⇒
      m.onConnectionAdd(connection)
    }

    onErrorList.collectFirst({ case e: Left[Exception, Unit] ⇒ e }) match {
      case Some(e) ⇒ e

      case None ⇒
        /*filtering block*/
        val filteredCon: Rx[Option[Connection[_]]] = connectionFilters.foldLeft[Rx[Option[Connection[_]]]](
          Var {Option(connection)}
        )((c, f) ⇒ f(c))
        _connectionsRx() = filteredCon :: (_connectionsRx.now: List[Rx[Option[Connection[_]]]])
        /* end block */
        _connections() = connection :: _connections.now
        _lastAddedConnection() = filteredCon
        //        println(s"added connection ${connection} to ${this} ${_connections.now}")

        onErrorList = Stream(getModules({ case m: ActionAfterGraphTransform ⇒ m }): _*) map { m ⇒
          m.onConnectionAdd(connection)
        }
        onErrorList.collectFirst({ case e: Left[Exception, Unit] ⇒ e }) match {
          case Some(e) ⇒ e
          case None ⇒ Right()
        }
    }
  }

  /** must be filled before accessed */
  private var onConnectionsRequestedModules: List[ActionOnConnectionsRequest] = null

  protected val modulesFinishedLoading = Var(false)

  /* Constructor */
  _modules = ModuleRegistry.getModules(this)
  onConnectionsRequestedModules = getModules({ case m: ActionOnConnectionsRequest ⇒ m })
  console.trace(s"Loaded modules ${_modules}")
  modulesFinishedLoading() = true
}


trait Module[+T <: Octopus] {
  val withinOctopus: T
}