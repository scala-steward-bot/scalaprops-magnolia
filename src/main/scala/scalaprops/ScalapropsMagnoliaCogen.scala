package scalaprops

import magnolia1.{CaseClass, Magnolia, SealedTrait}
import scala.annotation.tailrec

object ScalapropsMagnoliaCogen {
  type Typeclass[T] = scalaprops.Cogen[T]

  def join[T](ctx: CaseClass[Typeclass, T]): Typeclass[T] =
    new Cogen[T] {
      def cogen[B](t: T, g: CogenState[B]) = {
        ctx.parameters.zipWithIndex.foldLeft(g) { case (state, (p, i)) =>
          Cogen[Int].cogen(i, Cogen[String].cogen(p.label, p.typeclass.cogen(p.dereference(t), state)))
        }
      }
    }

  def split[T](ctx: SealedTrait[Typeclass, T]): Typeclass[T] =
    new Cogen[T] {
      def cogen[B](t: T, g: CogenState[B]) = {
        val index = ctx.subtypes.indexWhere(_.cast.isDefinedAt(t))
        if (index >= 0) {
          @tailrec
          def loop(i: Int, r: Rand): Rand = {
            if (i <= 0) r
            else loop(i - 1, g.gen.f(index, r)._1)
          }
          val newR = loop(index, g.rand)
          val s = ctx.subtypes(index)
          Cogen[String].cogen(s.typeName.full, s.typeclass.cogen(s.cast(t), g.copy(rand = newR)))
        } else {
          sys.error(s"bug? $ctx $t $g $index")
        }
      }
    }

  implicit def derivingScalapropsCogen[T]: Typeclass[T] =
    macro Magnolia.gen[T]
}
