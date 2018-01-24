package life.plenty.model.octopi

import life.plenty.model.connection.Marker
import life.plenty.model.connection.MarkerEnum.FILL_GREAT_QUESTIONS
import life.plenty.model.utils.InstantiateByApply

trait Question extends Space with WithParent[Space]

class BasicQuestion(override val _parent: Space, override val _title: String) extends Question {
  //  override def preConstructor(): Unit = {
  //    super.preConstructor()
  //    //println("BasicQuestion constr", this.connections)
  //  }
}

object BasicQuestion extends InstantiateByApply[BasicQuestion] {
  override def instantiate: BasicQuestion = new BasicQuestion(null, null)
}

class BasicSpace(override val _title: String) extends Space with WithMembers {

  override protected def preConstructor(): Unit = {
    super.preConstructor()
    addConnection(Marker(FILL_GREAT_QUESTIONS))
    //println("basic space init", this.connections)
  }
}

object BasicSpace extends InstantiateByApply[BasicSpace] {
  def instantiate = new BasicSpace(null)
}


