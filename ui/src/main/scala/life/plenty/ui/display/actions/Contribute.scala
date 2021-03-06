package life.plenty.ui.display.actions

import com.thoughtworks.binding.{Binding, dom}
import life.plenty.model.Contribution
import life.plenty.model.actions.ActionAddContributor
import life.plenty.ui.model.DisplayModel
import life.plenty.ui.model.DisplayModel.DisplayModule
import org.scalajs.dom.Event
import org.scalajs.dom.raw.Node

class Contribute(override val withinOctopus: Contribution) extends DisplayModule[Contribution] {
  override def update(): Unit = Unit
  @dom
  override protected def generateHtml(overrides: List[DisplayModel.ModuleOverride]): Binding[Node] = {
    val action = findAction

    <div>
      <input type="button" value="sign-up" disabled={action.isEmpty} onclick={e: Event ⇒
        findAction.foreach(a => {
          a.add("test-user-id")
        })}></input>
    </div>
  }
  private def findAction = withinOctopus.getTopModule({ case m: ActionAddContributor ⇒ m })
}
