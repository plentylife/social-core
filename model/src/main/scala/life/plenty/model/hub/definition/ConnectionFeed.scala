package life.plenty.model.hub.definition

import life.plenty.model.connection.{Active, DataHub, Inactive}
import monix.reactive.{MulticastStrategy, Observable}
import monix.reactive.OverflowStrategy.Unbounded
import monix.reactive.observers.Subscriber
import monix.execution.cancelables.SingleAssignCancelable
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.Future

trait ConnectionFeed {self: ConnectionManager ⇒
  val (insertSub, inserts) = Observable.multicast[DataHub[_]](MulticastStrategy.publish)
  val (removeSub, removes) = Observable.multicast[DataHub[_]](MulticastStrategy.publish)

  val (insertSubC, insertsC) = Observable.multicast[DataHub[_]](MulticastStrategy.replay)
  val (removeSubC, removesC) = Observable.multicast[DataHub[_]](MulticastStrategy.replay)

  def onInsert(con: DataHub[_]) = {
//    println(s"Inserting connection ${con.isActive}")
    if (con.isActive) {
      insertSub.onNext(con)
      insertSubC.onNext(con)
    }
    con.removed.foreach(r ⇒ if (r) removeSub.onNext(con) else insertSub.onNext(con))
    con.removed.foreach(r ⇒ if (r) removeSubC.onNext(con) else insertSubC.onNext(con))
  }

  def getLoadedStreamingList[T](extractor: PartialFunction[DataHub[_], T]): Future[ObsStream[T]] =
    self.conExList(extractor) map {list ⇒
    new ObsStream[T] {
      override val _inserts: Observable[T] = Observable.fromIterable(list) ++ inserts.collect(extractor)
      override val _removes: Observable[T] = removes.collect(extractor)
    }
  }

  def getStreamingList[T](extractor: PartialFunction[DataHub[_], T]): ObsStream[T] = {
    connections // called purely to get the loading going
//    loadCompleted foreach {_ ⇒
//      println(s"LOAD completed cons ${connections}")
//    }
    insertsC.foreach(i ⇒ println(s"INSC ${i}"))
    inserts.foreach(i ⇒ println(s"INS ${i}"))

    new ObsStream[T] {
      override val _inserts: Observable[T] = insertsC.collect(extractor)
      override val _removes: Observable[T] = removesC.collect(extractor)
    }
  }


  val removed = inserts.collect {
    case Inactive(_) ⇒ -1
    case Active(_) ⇒ 1
  }.scan(0)((s, ai) ⇒ s + ai).map(_ < 0)
}
