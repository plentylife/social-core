package life.plenty

import life.plenty.model._
import life.plenty.model.octopi._
import life.plenty.ui.actions.DisplayUpdateOnChildrenTransform
import life.plenty.ui.display._
import life.plenty.ui.display.actions.CreateAnswer
import life.plenty.ui.display.meta.{ChildDisplay, ModularDisplay}
import life.plenty.ui.filters.BasicSpaceDisplayOrder

package object ui {

  val thanks = "\u20B8"

  def initialize(): Unit = {
    println("UI is adding modules into registry")

    /* the modules should be added in a list fashion: the last overrides the first */
    ModuleRegistry add { case o: Members ⇒ new MembersDisplay(o) }

    ModuleRegistry add { case o: Space ⇒ new CurrentUserWallet(o) }
    ModuleRegistry add { case o: Space ⇒ new MenuBar(o) }
    //    ModuleRegistry add { case o: Space ⇒ new ViewStateLinks(o) }
    //    ModuleRegistry add { case o: BasicSpace ⇒ new RateEffortDisplay(o) }
    ModuleRegistry add { case o: BasicSpace ⇒ new BasicSpaceDisplayOrder(o) }
    ModuleRegistry add { case o: Space ⇒ new GreatQuestionGroup(o) }

    ModuleRegistry add { case o: GreatQuestion ⇒ new TitleWithQuestionInput(o) }

    ModuleRegistry add { case q: Question ⇒ new QuestionTitle(q) }
    ModuleRegistry add { case q: Question ⇒ new CreateAnswer(q) }

    ModuleRegistry add { case a: BasicAnswer ⇒ new BasicAnswerDisplay(a) }
    ModuleRegistry add { case c: Contribution ⇒ new ContributionDisplay(c) }
    // following the model of one contributer per contribution/answer
    //    ModuleRegistry add { case c: Contribution ⇒ new Contribute(c) }

    //    ModuleRegistry add { case o: Octopus ⇒ new RateEffortModuleFilter(o) }
    //    ModuleRegistry add { case o: Octopus ⇒ new DiscussModuleFilter(o) }
    //    ModuleRegistry add { case o: Octopus ⇒ new RateEffortConnectionFilter(o) }

    ModuleRegistry add { case o: Octopus ⇒ new DisplayUpdateOnChildrenTransform(o) }
    ModuleRegistry add { case o: Octopus ⇒ new ChildDisplay(o) }

    ModuleRegistry add { case o: Octopus ⇒ new ModularDisplay(o) }
    ModuleRegistry add { case o: Question ⇒ new QuestionModuleGroup(o) }
  }

}
