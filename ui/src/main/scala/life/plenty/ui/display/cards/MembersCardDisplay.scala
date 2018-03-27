package life.plenty.ui.display.cards

import scala.concurrent.ExecutionContext.Implicits.global
import com.thoughtworks.binding.{Binding, dom}
import life.plenty.data.DbReaderModule
import life.plenty.model.hub.definition.Hub
import life.plenty.model.hub.{Contribution, Members, Space, User}
import life.plenty.model.utils.GraphExtractorsDEP
import life.plenty.ui
import life.plenty.ui.display.utils.CardNavigation
import life.plenty.ui.display.utils.Helpers.{BasicBindable, BindableModule, _}
import life.plenty.ui.display.Help
import life.plenty.ui.display.user.FullUserBadge
import life.plenty.ui.model.{DisplayModule, UiContext}
import org.scalajs.dom.Event
import org.scalajs.dom.raw.Node
import rx.{Ctx, Rx, Var ⇒ rxVar}
import scalaz.std.list._

class MembersCardDisplay(override val hub: Members) extends DisplayModule[Members] with CardNavigation {
  private var addedCurrentUser = false

  private lazy val members: BasicBindable[List[User]] = hub.getMembers

  override def update(): Unit = {
    // fixme this will need to go
    if (!addedCurrentUser) {
      hub.whenLoadComplete foreach {_ ⇒
        ui.console.trace(s"Trying to add member ${UiContext.userVar.value} to $hub")
        hub.addMember(UiContext.userVar.value)
        addedCurrentUser = true
      }
    }
  }

  @dom
  override protected def generateHtml(): Binding[Node] = {
    <div class="card d-inline-flex members" id={hub.id}>
      <span class="d-flex header-block">
        <span class="d-flex title-block">
          <h5 class="card-title">Earnings</h5>
        </span>
        <span class="card-controls">
          <div class="btn-help d-inline-flex" onclick={_: Event ⇒ Help.membersCardHelp}>help</div>
          <div class="btn btn-primary btn-sm open-btn" onclick={navigateTo _}>open</div>        </span>
      </span>

      <div class="card-body">
          {for (m <- members().bind) yield displayMember(m).bind}
      </div>
    </div>
  }

  private val contributions: Rx[List[Contribution]] = Rx {hub.getParent() match {
    case Some(p: Space) ⇒ val l = GraphExtractorsDEP.getAllContributionsInSpace(p); l()
    case _ ⇒ List()
  }}
  private lazy val maxEarned: rxVar[Int] = rxVar(0)

  private def displayMember(u: User): Binding[Node] = {
    val userContributions = Rx {
      contributions().filter(_.getCreator().exists(_.id == u.id))
    }
    val userEarned = userContributions.map(list ⇒ {
      (0 :: list.map(_.tips())).sum
    })
    userEarned.foreach {ue ⇒ if (ue > maxEarned.now) maxEarned() = ue}
    val b = new BadgeMemberEarned(u, userContributions, userEarned = userEarned, maxEarned, this)

    b.html()
  }
}


class BadgeMemberEarned(val user: User, contributions: Rx[List[Contribution]],
                        userEarned: Rx[Int], maxEarned: Rx[Int],
                        caller: DisplayModule[Hub])
                       (implicit ctx: Ctx.Owner) {
  private lazy val badgeDisplay = new BindableModule(user.getTopModule({case m: FullUserBadge ⇒ m}), caller)
  private lazy val contributionsCount = contributions.map(_.size)
  private lazy val barWidth: BasicBindable[Double] = Rx {
    if (maxEarned() == 0) 0 else userEarned().toDouble / maxEarned()
  }

  @dom
  def html(): Binding[Node] = {
    val widthStyle = s"width: ${barWidth().bind * 100}%"

    <div class="d-flex user-space-contribution-box">
      <div class="user-box">
      {badgeDisplay.dom.bind}
        <div class="badge badge-secondary">
          {contributionsCount.dom.bind} contributions
        </div>
      </div>

      <div class="user-earned-bar-box">
        {new BindableProperty(userEarned)(_.toString + ui.thanks).dom.bind}
        <div class="user-earned-bar" style={widthStyle}></div>
      </div>

    </div>
  }
}