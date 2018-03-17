package life.plenty.model.hub.definition

import monix.reactive.Observable

import scala.concurrent.Future
import monix.execution.Scheduler.Implicits.global

trait ObsStream[T] {
  val _inserts: Observable[T]
  val _removes: Observable[T]

  // fixme the removes are dumb
  def flatCombine[B](f: T ⇒ ObsStream[B]): ObsStream[B] = {
    val m = _inserts.map(e ⇒ e → f(e))
    val r = _removes.flatMap {e ⇒
      val obs = m.findF(_._1 == e).map(_._2)
      obs.flatMap(os ⇒ os._inserts ++ os._removes)
    }
    val i = m flatMap {_._2._inserts}
    _inserts.toListL.foreach(_il ⇒
      i.toListL.foreach(il ⇒
        println(s"FLAT COMBINE ${_il} ${il}")
      )
    )
    ObsStream(i, r)
  }

  def ++ (other: ObsStream[T]): ObsStream[T] = {
    ObsStream(_inserts ++ other._removes, _removes ++ other._removes)
  }
}

object ObsStream {
  def apply[T](in: Observable[T], rem: Observable[T]): ObsStream[T] = new ObsStream[T] {
    override val _inserts: Observable[T] = in
    override val _removes: Observable[T] = rem
  }
}