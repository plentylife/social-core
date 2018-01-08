package life.plenty.model.actions

import life.plenty.model._

class ActionCreateQuestion(override val withinOctopus: Question) extends Module[Question] {
  def create(title: String) = {
    val q = new BasicQuestion(withinOctopus, title)
    println("created question ", q, "in", withinOctopus, withinOctopus.connections)
    withinOctopus.addConnection(Child(q))
  }
}