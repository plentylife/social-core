package life.plenty.model.octopi.definition

import java.util.Date

import life.plenty.model
import life.plenty.model.actions.ActionOnNew
import life.plenty.model.connection._
import life.plenty.model.octopi.User
import life.plenty.model.utils._
import rx.{Rx, Var}

import scala.util.Random

trait OctopusConstructor {
  self: Octopus ⇒

  //  implicit private[this] val ctxConstructor: Ctx.Owner = self.ctx

  private val rand = Random

  def getRxId: Rx[Option[String]] = rx.get({ case Id(id) ⇒ id })

  /** Either retrieves the id, or generates a new one, and sets it */
  def id: String = sc.ex({ case Id(id) ⇒ id }) getOrElse {
    val gid = model.getHasher.b64(generateId)
    setInit(Id(gid))
    gid
  }

  protected def generateId: String = {
    val res = rand.nextLong().toString +
      sc.exf({ case CreationTime(t) ⇒ t }).toString + sc.exf({ case Creator(c) ⇒ c }).id
    res
  }

  lazy val getCreationTime: Rx[Option[Long]] = rx.get({ case CreationTime(t) ⇒ t })

  lazy val getCreator: Rx[Option[User]] = rx.get({ case Creator(t) ⇒ t })

  private var _required: Set[() ⇒ Rx[Option[_]]] = Set(() ⇒ getCreator)

  def addToRequired(r: ⇒ Rx[Option[_]]) = _required += { () ⇒ r }

  def clearRequired() = _required = Set()

  final def required: Set[() ⇒ Rx[Option[_]]] = _required

  /** alias for [[addConnection()]] with the connection marked */
  def setInit(c: Connection[_]): Unit = addConnection(c.inst)

  private lazy val isNewVar = Var(false)

  def isNew = isNewVar.now

  def onNew(f: ⇒ Unit): Unit = {
    isNewVar.foreach(i ⇒ {
      if (i) {
        f
        isNewVar.kill()
      }
    })
  }

  def onModulesLoad(f: ⇒ Unit): Unit = {
    modulesFinishedLoading.foreach(i ⇒ {
      if (i) {
        model.console.println("Modules finished loading. Executing function.")
        f
        modulesFinishedLoading.kill()
      }
    })
  }

  def asNew(properties: Connection[_]*): Unit = {
    model.console.trace(s"attempting to instantiate ${this.getClass} with creator ${model.defaultCreator}")
    properties.foreach(p ⇒ {
      p.tmpMarker = AtInstantiation
      self.setInit(p)
    })
    model.console.trace("New octopus has connections")
    val ct = CreationTime(new Date().getTime)
    ct.tmpMarker = AtInstantiation
    addConnection(ct)

    if (!properties.exists(_.isInstanceOf[Creator])) {
      model.defaultCreator.foreach(c ⇒ setInit(Creator(c).inst))
    }
    for (p ← required) {
      if (p().now.isEmpty) throw new Exception(s"Class ${this.getClass} was not properly instantiated. " +
        s"Connections ${this._connections.now}")
    }

    getCreator.addConnection(Created(this).inst)
    isNewVar() = true

    getModules({ case m: ActionOnNew[_] ⇒ m }).foreach({_.onNew()})
    model.console.println(s"successfully instantiated ${this} ${this.id}")
  }
}