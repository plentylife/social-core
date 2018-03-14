package life.plenty.ui.display.feed

import com.thoughtworks.binding.{Binding, dom}
import life.plenty.model.connection.MarkerEnum.MarkerEnum
import life.plenty.model.connection.{Child, Marker}
import life.plenty.model.octopi.Space
import life.plenty.model.octopi.definition.Hub
import life.plenty.model.utils.GraphUtils
import life.plenty.model.utils.GraphUtils.collectDownTree
import life.plenty.ui.display.actions.OpenButton
import life.plenty.ui.display.meta.LayoutModule
import life.plenty.ui.display.utils.CardNavigation
import life.plenty.ui.display.utils.Helpers._
import life.plenty.ui.display.{CardQuestionDisplayBase, FullUserBadge, InlineQuestionDisplay}
import life.plenty.ui.model.{ComplexModuleOverride, DisplayModel, ExclusiveModuleOverride, ModuleOverride}
import org.scalajs.dom.Node

class SpaceFeedDisplay(override val hub: Space) extends LayoutModule[Space] with CardNavigation {
  private lazy val aggregated = collectDownTree[Hub](hub, matchBy = {
    case Child(h: Hub) ⇒ h
    case m: Marker ⇒ m
  },
    allowedPath = {case Child(h: Hub) ⇒ h})
  private lazy val aggregatedB = new ListBindable(aggregated)

  // todo. display confirmed

  @dom
  override protected def generateHtml(): Binding[Node] = {
    val cos: Seq[ModuleOverride] = this.cachedOverrides.bind
    val selfOs = ExclusiveModuleOverride(m => !(m.isInstanceOf[FeedDisplay[_]] || m.isInstanceOf[FullUserBadge]))
    implicit val os = selfOs :: cos.toList ::: siblingOverrides
//    implicit val os = cos.toList ::: siblingOverrides
    val cssClass = ""

    <div class={"card d-inline-flex flex-column space " + cssClass} id={hub.id}>
      <span class="d-flex header-block">
        <span class="d-flex title-block" onclick={navigateTo _}>
          <h5 class="card-title">
            {hub.getTitle.dom.bind}
          </h5>
        </span>

        <span class="card-controls">
          {displayModules(siblingModules.withFilter(m => m.isInstanceOf[OpenButton]), "modules").bind}
        </span>
      </span>

      <div class="card-body">
        {for (c <- aggregatedB()) yield {
          DisplayModel.display(c, os, this).bind
      }}
      </div>

    </div>
  }
}
