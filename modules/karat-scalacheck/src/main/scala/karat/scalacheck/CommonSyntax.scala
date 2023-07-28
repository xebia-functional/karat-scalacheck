package karat.scalacheck

import karat.concrete.FormulaKt
import karat.scalacheck.Scalacheck.{Atomic, Formula, Predicate, Remember}
import org.scalacheck.Prop

trait CommonSyntax {

  def should[A](test: A => Boolean): Predicate[A] = FormulaKt.predicate { (a: A) =>
    if (test(a)) Prop.Result(Prop.True) else Prop.Result(Prop.False)
  }
  def predicate[A](test: A => Prop.Result): Predicate[A] = FormulaKt.predicate { (a: A) =>
    test(a)
  }
  def always[A](value: Formula[A]): Formula[A] = FormulaKt.always(value)
  def next[A](value: Formula[A]): Formula[A] = FormulaKt.next(value)
  def eventually[A](value: Formula[A]): Formula[A] = FormulaKt.eventually(value)
  def implies[A](condition: Atomic[A], `then`: Formula[A]): Formula[A] = FormulaKt.implies(condition, `then`)
  def remember[A](block: A => Formula[A]): Remember[A] = FormulaKt.remember { (a: A) =>
    block(a)
  }

  def afterwards[A](formula: Formula[A]): Formula[A] = next(always(formula))

}
