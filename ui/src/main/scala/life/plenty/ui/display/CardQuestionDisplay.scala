package life.plenty.ui.display

import com.thoughtworks.binding.{Binding, dom}
import life.plenty.model.octopi.definition.Hub
import life.plenty.model.octopi.{Answer, Question, Space}
import life.plenty.model.utils.GraphUtils
import life.plenty.model.utils.GraphUtils._
import life.plenty.ui.display.actions.SpaceActionsBar
import life.plenty.ui.display.meta.LayoutModule
import life.plenty.ui.display.utils.CardNavigation
import life.plenty.ui.model.DisplayModel.DisplayModule
import life.plenty.ui.display.utils.Helpers._
import life.plenty.ui.model.{ComplexModuleOverride, ModuleOverride, Router, UiContext}
import org.scalajs.dom.Node

//{displayModules(siblingModules.withFilter(_.isInstanceOf[SpaceActionsBar]), "card-space-menu").bind}

class CardQuestionDisplay(override val hub: Question) extends LayoutModule[Question] with CardNavigation {
  override def doDisplay() = true

  private lazy val isConfirmed: BasicBindable[Boolean] = GraphUtils.markedConfirmed(hub)

  @dom
  override protected def generateHtml(): Binding[Node] = {
    val cos: Seq[ModuleOverride] = this.cachedOverrides.bind
    val inlineQuestins =
      ComplexModuleOverride(this, {case m: InlineQuestionDisplay ⇒ m}, _.isInstanceOf[CardQuestionDisplay])
    implicit val os = inlineQuestins :: cos.toList ::: siblingOverrides
    var confirmedCss = if (isConfirmed().bind) " confirmed " else ""


    <div class={"card d-inline-flex flex-column question " + confirmedCss}
         id={hub.id}>
      <span class="d-flex header-block" onclick={navigateTo _}>
        <span class="d-flex title-block">
          <h5 class="card-title">{hub.getTitle.dom.bind}</h5>
          <div class="card-subtitle mb-2 text-muted">
            {getBody(hub).dom.bind}
          </div>
        </span>
        <span class="card-controls">
          <div class="btn btn-primary btn-sm">open</div>
        </span>
      </span>

      <div class="card-body">

        {displayHubs(children.withFilter(_.isInstanceOf[Answer]), "answers").bind}
        {displayHubs(children.withFilter(_.isInstanceOf[Question]), "inner-questions").bind}

        </div>
    </div>
  }
}