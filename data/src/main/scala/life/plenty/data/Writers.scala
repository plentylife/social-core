package life.plenty.data

import life.plenty.data.Main.gun
import life.plenty.model.connection.Connection
import life.plenty.model.octopi.{Module, Octopus, Space}

import scala.scalajs.js

object OctopusWriter {
  def write(o: Octopus): Unit = {
    println(s"writing ${o} ${o.connections}")
    // fixme there should be a check that the class does not already exist
    val go = gun.get(o.id)
    go.put(js.Dynamic.literal(
      "class" → o.getClass.getSimpleName
    ), null)
    //    o.getTopModule({ case m: ConstructorWriterModule[_] ⇒ m }).foreach(_.write(go))

    val gcons = go.get("connections")
    for (c ← o.allConnections) {
      val conGun = ConnectionWriter.write(c)
      println("writing connection", c, c.id)
      gcons.set(conGun, null)
    }
  }
}

object ConnectionWriter {
  def write(c: Connection[_]): Gun = {
    val gc = Main.gun.get(c.id)
    gc.`val`((d, k) ⇒ {
      if (js.isUndefined(d)) {
        val v = getValue(c)
        c.value match {
          case o: Octopus ⇒ OctopusWriter.write(o)
          case _ ⇒
        }
        val obj = js.Dynamic.literal(
          "class" → c.getClass.getSimpleName,
          "value" → v
        )
        gc.put(obj)
      }
    })
  }

  def getValue(c: Connection[_]) = {
    c.value match {
      case o: Octopus ⇒ o.id
      case other ⇒ other.toString()
    }
  }
}

trait ConstructorWriterModule[T <: Octopus] extends Module[T] {
  def write(gun: Gun)
}

class SpaceConstructorWriter(override val withinOctopus: Space) extends ConstructorWriterModule[Space] {
  def write(gun: Gun) = {
    val constr = gun.get("class-constructor")
    constr.put(js.Dynamic.literal(
      "title" → withinOctopus.title()
    ), null)
  }
}