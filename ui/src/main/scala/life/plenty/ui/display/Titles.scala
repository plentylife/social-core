package life.plenty.ui.display

import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.{Binding, dom}
import life.plenty.model.Space
import life.plenty.ui.model.DisplayModel.{DisplayModule, ModuleOverride, TitleDisplay}
import org.scalajs.dom.raw.Node


class TitleWithNav(override val withinOctopus: Space) extends DisplayModule[Space] with TitleDisplay {
  override def overrides: List[ModuleOverride] = super.overrides ::: List(
    ModuleOverride(new NoDisplay(withinOctopus), (m) ⇒ m.isInstanceOf[TitleWithNav]))

  @dom
  override def displaySelf(overrides: List[ModuleOverride]): Binding[Node] = {
    println("octopus titlewithnav", withinOctopus, overrides)
    <div class="nav-bar">
      <div>back</div>
      <div class="title">
        {Var(withinOctopus.title).bind}
      </div>
    </div>
  }
}

class TitleWithInput(override val withinOctopus: Space) extends DisplayModule[Space] with TitleDisplay {
  @dom
  override def displaySelf(overrides: List[ModuleOverride]): Binding[Node] = {
    <div class="title-with-input">
      <div class="title">
        {Var(withinOctopus.title).bind}
      </div>
      <input type="text"/>
    </div>
  }
}
