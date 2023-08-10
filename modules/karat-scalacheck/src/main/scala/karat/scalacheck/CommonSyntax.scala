/*
 * Copyright 2023 Xebia Functional Open Source <https://www.xebia.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package karat.scalacheck

import karat.concrete.FormulaKt
import karat.scalacheck.Scalacheck.{Atomic, Formula, Predicate, Remember}
import org.scalacheck.Prop

trait CommonSyntax {

  def should[A](test: A => Boolean): Predicate[A] = FormulaKt.predicate { (a: A) =>
    if (test(a)) Prop.Result(Prop.True) else Prop.Result(Prop.False)
  }
  def predicate[A](test: A => Prop.Result): Predicate[A] = FormulaKt.predicate(test(_: A))
  def always[A](value: Formula[A]): Formula[A] = FormulaKt.always(value)
  def next[A](value: Formula[A]): Formula[A] = FormulaKt.next(value)
  def eventually[A](value: Formula[A]): Formula[A] = FormulaKt.eventually(value)
  def implies[A](condition: Atomic[A], `then`: Formula[A]): Formula[A] = FormulaKt.implies(condition, `then`)
  def remember[A](block: A => Formula[A]): Remember[A] = FormulaKt.remember(block(_: A))
  def afterwards[A](formula: Formula[A]): Formula[A] = next(always(formula))

}
