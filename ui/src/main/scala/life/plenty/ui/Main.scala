package life.plenty.ui

import com.thoughtworks.binding.{Binding, dom}
import life.plenty.data.{OctopusReader, Main ⇒ dataMain}
import life.plenty.model.octopi._
import life.plenty.model.{initialize ⇒ mInit}
import life.plenty.ui.display.{Help, Login}
import life.plenty.ui.model.{DisplayModel, UiContext}
import org.scalajs.dom.raw.Node
import org.scalajs.dom.{Event, document}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scalaz.std.list._
@JSExportTopLevel("Main")
object Main {

  @JSExport
  def main(): Unit = {
    println("Entry point")

    // has to be first because it sets the hasher function
    dataMain.main()
    UiContext.initialize()
    //    Router.initialize
    mInit()
    initialize()

    dom.render(document.body, mainSection())
    if (UiContext.getUser != null) showUi()
  }

  def showUi() = {
    val id = "02PgqlznC6LmE6FJNettMmLVxztTemvxdb3ChgXTsOk="
    println(s"UI loading ${id}")
    OctopusReader.read(id) foreach { spaceOpt ⇒
      UiContext.startingSpace.value_=(spaceOpt map { s ⇒ s.asInstanceOf[Space] })
    }
  }

  @dom
  def mainSection(): Binding[Node] = {
    <div id="viewport" onclick={e: Event ⇒ Help.triggerClose()}>
      {Help.display().bind}{Login.display().bind}{if (UiContext.startingSpace.bind.nonEmpty)
      DisplayModel.display(UiContext.startingSpace.bind.get).bind else
      <span>nothing to show</span>}
    </div>
  }

}
