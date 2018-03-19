package life.plenty.ui.display.utils

import com.thoughtworks.binding.Binding.{Var, Vars}
import com.thoughtworks.binding.{Binding, dom}
import life.plenty.model.connection.DataHub
import life.plenty.model.hub.definition.Hub
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.scalajs.dom.Node
//
//class DomStream[T](hub: Hub, extractor: PartialFunction[DataHub[_], T]) {
//  val v = Vars[T]()
//
//  hub.conExList(extractor) foreach { list ⇒
//    v.value.insertAll(0, list)
//    hub.feed.collect(extractor).foreach(dh ⇒ v.value.insert(0, dh))
//    hub.removes.collect(extractor).foreach(dh ⇒ v.value.remove(v.value.indexOf(dh)))
//  }
//
//}

class DomStream(stream: Observable[Hub]) {
  val v = Vars[Hub]()
  stream.foreach(h ⇒ v.value.insert(0, h))
}

class DomValStream[T <% String](stream: Observable[T]) {
  val v = Var[Option[T]](None)
  stream.foreach(h ⇒ v.value_=(Option(h)))

  @dom
  def dom: Binding[String] = {
    v.bind match {
      case Some(value) ⇒ (value : String)
      case None ⇒ ""
    }
  }
}
