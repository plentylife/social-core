package life.plenty.ui.display.cards

import com.thoughtworks.binding.Binding.Var
import com.thoughtworks.binding.{Binding, dom}
import life.plenty.model.actions.ActionGiveThanks
import life.plenty.model.hub.{Contribution, User}
import life.plenty.ui
import life.plenty.ui.display.actions.{ChangeParent, EditSpace}
import life.plenty.ui.display.utils.Helpers._
import life.plenty.ui.model.{DisplayModule, UiContext}
import org.scalajs.dom.Event
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.Node
import rx.{Obs, Rx}

import scala.util.{Failure, Success, Try}

@deprecated
class ContributionDisplay(override val hub: Contribution) extends DisplayModule[Contribution] {
  //  protected val body = Var[String](withinOctopus.body())
  private val tipsCollected = Var(0)
  private var tipsCollectedRx: Obs = null
  private val open = Var(false)
  private var tipping: Int = 1
  private var error = Var("")

  private lazy val editor: BindableAction[EditSpace] = new BindableAction(hub.getTopModule({ case
    m: EditSpace ⇒ m
  }), this)

  override def update(): Unit = {
    if (tipsCollectedRx == null) {
      tipsCollectedRx = hub.tips.foreach(t ⇒ tipsCollected.value_=(t))
    }
  }

  @dom
  protected override def generateHtml(): Binding[Node] = {
    val disabled = findTipModule.isEmpty
    <div class="card d-inline-flex mt-1 mr-1 flex-column contribution">
      <div class="d-inline-flex flex-row flex-nowrap">

        <div class="d-inline-flex flex-column controls">
          {if (error.bind.nonEmpty) {
          <div class="text-danger">
            {error.bind}
          </div>
        } else {
          <span></span>
        }}{if (open.bind) {inputDisplay.bind} else <span></span>}<button type="button" class="btn btn-primary btn-sm"
                                                                         disabled={disabled}
                                                                         onclick={onTip _}>Tip</button>
          <span>collected
            {s"${tipsCollected.bind} ${ui.thanks}hanks"}
          </span>
        </div>
        <div class="card-body">
          <h6 class="card-title">contribution</h6>
          <h6 class="card-subtitle mb-2 text-muted">by
            {val c: Rx[Option[String]] = hub.getCreator.map((optU: Option[User]) => optU.map {
            u: User => u.getNameOrEmpty(): String
          });
          c.dom.bind}
          </h6>
          <p class="card-text">
            {hub.getBody.dom.bind}
          </p>
        </div>
      </div>

      <div class="card-controls-bottom d-flex">
        {ChangeParent.displayActiveOnly(hub).bind}{editor.dom.bind}
      </div>

    </div>
  }

  private lazy val findTipModule = hub.getTopModule({ case m: ActionGiveThanks ⇒ m })

  private def onTip(e: Event) = {
    if (open.value) {
      if (tipping > 0) {
        findTipModule.foreach(_.add(tipping, UiContext.getUser))
        tipping = 1
        open.value_=(false)
      } else {
        error.value_=("must tip more than nothing")
      }
    } else open.value_=(!open.value)
  }

  @dom
  private def inputDisplay: Binding[Node] = <span>
    <input type="text" class="tip-input" value={tipping.toString}
           oninput={e: Event => onEnterTip(e)}></input>{ui.thanks}
  </span>

  private def onEnterTip(e: Event) = {
    val v = e.target.asInstanceOf[Input].value
    Try(v.toInt) match {
      case Success(t: Int) ⇒ tipping = t; error.value_=("")
      case Failure(_) ⇒ error.value_=("Not a round number")
    }
  }
}