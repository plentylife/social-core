package life.plenty.model.actions

import life.plenty.model.connection.MarkerEnum.{FILL_GREAT_QUESTIONS, HAS_FILLED_GREAT_QUESTIONS}
import life.plenty.model.connection.{Child, Marker, Parent}
import life.plenty.model.{GreatQuestion, GreatQuestions, Octopus, Space}

/* fixme move*/
class AddGreatQuestions(override val withinOctopus: Space) extends ActionOnInitialize[Space] {
  override def onInitialize(): Unit = {
    withinOctopus.getTopConnection({ case m@Marker(FILL_GREAT_QUESTIONS) ⇒ m }).foreach(_ ⇒ fill())
    //    println("added great questions to ", withinOctopus, withinOctopus.connections)
  }

  private def fill(): Unit = {
    // making sure that there isn't an infinite loop
    val p = withinOctopus.getTopConnectionData({ case Parent(p: Octopus) ⇒ p })
    val filled = p.exists(_.hasMarker(HAS_FILLED_GREAT_QUESTIONS)) ||
      withinOctopus.hasMarker(HAS_FILLED_GREAT_QUESTIONS)
    if (filled) {
      //println("aborted gq fill")
      return
    }
    withinOctopus addConnection Marker(HAS_FILLED_GREAT_QUESTIONS)

    // reverse because first in -- last out
    GreatQuestions.orderedConstructors.reverse map {
      constr ⇒ constr(withinOctopus)
    } foreach {
      question ⇒
        if (!withinOctopus.connections.collect(
          { case Child(q: GreatQuestion) ⇒
            //println(q.getClass, question.getClass)
            q
          }
        ).exists(_.getClass == question.getClass)) withinOctopus addConnection Child(question)
    }
  }
}
