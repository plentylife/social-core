package life.plenty.ui.display

import com.thoughtworks.binding.{Binding, dom}
import life.plenty.model.octopi.Space
import life.plenty.ui.display.meta.NoDisplay
import life.plenty.ui.model.DisplayModel.{DisplayModule, ModuleOverride}
import life.plenty.ui.model.Helpers._
import org.scalajs.dom.raw.Node

class MenuBar(override val withinOctopus: Space) extends DisplayModule[Space] with TitleDisplay {
  override def overrides: List[ModuleOverride] = super.overrides ::: List(
    ModuleOverride(this, new NoDisplay(withinOctopus), (m) ⇒ m.isInstanceOf[MenuBar]))

  override def update(): Unit = {
    super.update()
  }

  //  <div>back</div>

  @dom
  protected override def generateHtml(overrides: List[ModuleOverride]): Binding[Node] = {
    //    println("menu bar display")
    <div class="menu-bar d-flex flex-row align-items-center">

      <h3 class="title ml-2">
        {withinOctopus.getTitle.dom.bind}
      </h3>
      <div class="wallet">
        {CurrentUserWallet.generateHtml(withinOctopus).bind}
      </div>
    </div>
  }
}
