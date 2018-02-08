package life.plenty.ui.filters

import life.plenty.model.modifiers.OctopusOrdering
import life.plenty.model.octopi.definition.Octopus
import life.plenty.model.octopi.{BasicSpace, Members}
import rx.{Ctx, Rx}
//
//trait OrderPreference[IT, L<: Iterable[IT], T <: Octopus] extends Module[T] {
//  def orderPreference(toReorder: L): L
//}
//
//trait ChildOrderPreference extends OrderPreference[Octopus, List[Octopus], BasicSpace]

class BasicSpaceDisplayOrder(override val withinOctopus: BasicSpace) extends OctopusOrdering[BasicSpace] {
  private implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  override def order(what: List[Octopus]): List[Octopus] = {
    val i = what.indexWhere(_.isInstanceOf[Members])
    if (i != -1) {
      val s = what.splitAt(i)
      what(i) :: s._1 ::: s._2.tail
    } else {
      what
    }
  }

  override def applyRx(whatRx: Rx[List[Octopus]])(implicit ctx: Ctx.Owner): Rx[List[Octopus]] = whatRx.map(order)
}