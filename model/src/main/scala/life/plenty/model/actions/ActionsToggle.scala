package life.plenty.model.actions

import life.plenty.model.connection._
import life.plenty.model.octopi._
import life.plenty.model.octopi.definition.{Hub, Module}
import life.plenty.model.utils.GraphUtils
import rx.Ctx

class ActionSignup(override val hub: SignupQuestion) extends Module[SignupQuestion] {
  private implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  private lazy val contributing = GraphUtils.markedContributing(hub)

  def signup(who: User) = {
    val a = if (contributing.now) new Contribution() else new Proposal()
    a.asNew(Body(""), Parent(hub))
  }

  def designup(who: User) = ???
}

class ActionAddConfirmedMarker(override val hub: Hub) extends Module[Hub] {
  private implicit val ctx = hub.ctx

  def confirm() = {
    hub.addConnection(Marker(MarkerEnum.CONFIRMED))
    println(s"added confirm marker ${hub.sc.all}")
    println(s"${hub.rx.cons}")
  }

  def deconfirm() = {
    GraphUtils.confirmedMarker(hub).now.foreach { m ⇒
      println(s"marker is active ${m.isActive.now}")
      m.inactivate()

      println(s"removed marker ${m.sc.all}")
      println(s"marker is active ${m.isActive.now}")
      println(s"${hub.rx.cons}")
    }

  }
}

class ActionToggleCriticalConnection(override val hub: Hub) extends Module[Hub] {
  private implicit val ctx = hub.ctx
  private val critical = GraphUtils.getCritical(hub)

  def toggle(what: Hub) = {
    println(s"Toggle of critical $what")
    val existing = critical.now.find(_.value == what)

    existing foreach {c ⇒
      println(s"Toggle found $c ${c.isActive}")
      if(c.isActive.now) c.inactivate() else c.activate()
    }

    if (existing.isEmpty) {
      println(s"Toggle NOT found $what ${critical.now}")
      on(what)
    }
  }

//  def off(what: Hub) = {
//    println(s"Toggle off")
//    val existing = critical.now.find(_.id == what.id)
//
//    existing foreach {c ⇒
//      println(s"Toggle found $c ${c.isActive}")
//      if(c.isActive.now) c.inactivate() else c.activate()
//    }
//
//    if (existing.isEmpty) {
//      println(s"Toggle NOT found")
//      on(what)
//    }
//  }

  def on(what: Hub) = hub.addConnection(Critical(what))
}

class ActionAddContributor(override val hub: Contribution) extends Module[Contribution] {
  def add(userId: String) = {
    ???
    //    val u = new BasicUser(userId)
    //    val existing = withinOctopus.connections.collect({ case Contributor(u) ⇒ u })
    //    if (!existing.contains(u)) {
    //      withinOctopus.addConnection(Contributor(u))
    //      println("contributor added ", withinOctopus.connections)
    //    }
  }
}

class ActionAddMember(override val hub: WithMembers) extends Module[WithMembers] {

  def addMember(u: User) = {
    ???
  }

}

class ActionAddDescription(override val hub: Space) extends Module[Space] {
  private implicit val ctx = hub.ctx
  def add(body: String) = {
    val existing = hub.rx.get({ case c: Body ⇒ c })
    existing.foreach { cOpt ⇒
      existing.kill()
      cOpt foreach { c ⇒
        // fixme
//        withinOctopus.addConnection(Removed(c.id))
      }
    }
    hub.addConnection(Body(body))
  }
}