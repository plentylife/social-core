package life.plenty.data

import life.plenty.model.actions.ActionAfterGraphTransform
import life.plenty.model.connection.DataHub
import life.plenty.model.octopi.definition.{AtInstantiation, Hub, Module}
import life.plenty.model.security.SecureUser
import rx.Ctx

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.{Any, JSON}

class DbInsertConnectionOp(h: Hub) extends DbInsertOp {
  override val p: js.Array[js.Any] = js.Array("connections", 0)
  override val li: js.Any = h.id
}

object DbWriter {
  def writeInitial(o: Hub, doc: AsyncShareDoc): Unit = {
    console.println(s"OctopusWriter octopus ${o} ${o.id} ${o.sc.all}")
    if (Cache.getOctopus(o.id).nonEmpty) {
      console.println(s"OctopusWriter skipping octopus ${o} since it is in cache")
      return
    } else {
      // fixme this is danegerous because it does not check for success of the write
      Cache.put(o)
      o.tmpMarker = DbMarker
    }

    forceWriteInitial(o, doc)
  }

  private[data] def forceWriteInitial(o: Hub, doc: AsyncShareDoc, hubClass: Option[String] = None): Future[Unit] =
    doc.setInitial {
      val hc: String = hubClass.getOrElse(o.getClass.getSimpleName)
      val info = js.Dynamic.literal("class" → hc, "connections" → js.Array(
        o.sc.all.map(_.id):_*
      ))
      o match {
        case c: DataHub[_] ⇒ fillDataHubInfo(c, info)
        case _ ⇒
      }
      info
    }

  /** is not safe, but should never fail */
  def getDoc(h: Hub): AsyncShareDoc = h.getTopModule({case m: DbWriterModule ⇒ m}).get.dbDoc

  private def fillDataHubInfo(c: DataHub[_], info: js.Dynamic): Unit = {
    c.value match {
      case o: Hub ⇒
        info.updateDynamic("value")(o.id)
        info.updateDynamic("valueType")("hub")
      case other ⇒
        info.updateDynamic("value")(other.toString)
        info.updateDynamic("valueType")("string")
    }
  }

  def writeSingleConnection(holderDoc: AsyncShareDoc, connection: DataHub[_]): Unit = {
    DbWriter.writeInitial(connection, getDoc(connection))
    // add to holder
    holderDoc.submitOp(new DbInsertConnectionOp(connection))
  }

}

class DbWriterModule(override val hub: Hub) extends ActionAfterGraphTransform {
  private var _dbDoc: AsyncShareDoc = null
  protected[data] def dbDoc = if (_dbDoc != null) _dbDoc else {
    _dbDoc = new AsyncShareDoc(hub.id, true)
    _dbDoc
  }
  protected[data] def setDbDoc(doc: AsyncShareDoc) = _dbDoc = doc

  hub.onNew(onNew)

  protected def onNew = {
    console.println(s"Instantiation Gun Writer ${hub} ${hub.id}")
    DbWriter.writeInitial(hub, dbDoc)
  }

  override def onConnectionAdd(connection: DataHub[_]): Future[Unit] = {
    if (connection.tmpMarker != DbMarker && connection.tmpMarker != AtInstantiation) {
      console.println(s"Gun Writer onConAdd ${hub} [${hub.id}] ${connection} ")
      DbWriter.writeSingleConnection(dbDoc, connection)
    }
    Future {Right()}
  }
}

class SecureUserDbWriterModule(override val hub: SecureUser) extends DbWriterModule(hub) {
  override protected def onNew = {
    console.println(s"Instantiation Gun Writer forcing ${hub} ${hub.id}")
    DbWriter.forceWriteInitial(hub, dbDoc, Option("BasicUser"))
  }
}