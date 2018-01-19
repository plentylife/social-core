package life.plenty.ui.display

import com.thoughtworks.binding.Binding.Vars
import com.thoughtworks.binding.{Binding, dom}
import life.plenty.model.octopi.{Members, User}
import life.plenty.ui.model.DisplayModel
import life.plenty.ui.model.DisplayModel.DisplayModule
import org.scalajs.dom.raw.Node

class MembersDisplay(override val withinOctopus: Members) extends DisplayModule[Members] {
  private val _members = Vars[User]()

  override def update(): Unit = {
    _members.value.clear()
    _members.value.insertAll(0, withinOctopus.members)
  }

  @dom
  override protected def generateHtml(overrides: List[DisplayModel.ModuleOverride]): Binding[Node] = {
    <div class="card d-inline-flex mt-2 ml-2">
      <div class="card-body">

        <div class="card-title">members of this space:</div>
        <ul>
          {for (m <- _members) yield displayMember(m).bind}
        </ul>
      </div>
    </div>
  }

  @dom
  private def displayMember(u: User): Binding[Node] = <li>
    {u.id}
  </li>
}