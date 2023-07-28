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

import cats._
import cats.syntax.all._
import karat.concrete.progression.regular.{CheckKt, RegularStepResultManager}
import karat.concrete.progression.{Info, Step}
import kotlin.jvm.functions
import org.scalacheck.Prop

import scala.jdk.CollectionConverters._

object Scalacheck {
  type Atomic[A] = karat.concrete.Atomic[A, Prop.Result]
  type Formula[A] = karat.concrete.Formula[A, Prop.Result]
  type Predicate[A] = karat.concrete.Predicate[A, Prop.Result]
  type Remember[A] = karat.concrete.Remember[A, Prop.Result]

  class ScalacheckStepResultManager[A] extends RegularStepResultManager[A, Prop.Result, Prop.Result] {
    override def getEverythingOk: Prop.Result = Prop.Result(Prop.True)
    override def getFalseFormula: Prop.Result = Prop.Result(Prop.False)
    override def getUnknown: Prop.Result = Prop.Result(Prop.Undecided)
    override def isOk(result: Prop.Result): Boolean = result == null || result.success
    override def andResults(results: java.util.List[_ <: Prop.Result]): Prop.Result =
      results.asScala.fold(Prop.Result(Prop.True))((x: Prop.Result, y: Prop.Result) => x && y)
    override def orResults(results: java.util.List[_ <: Prop.Result]): Prop.Result =
      results.asScala.fold(Prop.Result(Prop.False))((x: Prop.Result, y: Prop.Result) => x || y)
    override def negationWasTrue(formula: karat.concrete.Formula[_ >: A, _ <: Prop.Result]): Prop.Result =
      Prop.Result(Prop.False).label("negation was true")
    override def shouldHoldEventually(formula: karat.concrete.Formula[_ >: A, _ <: Prop.Result]): Prop.Result =
      Prop.Result(Prop.False).label("should hold eventually")
    override def predicate(test: functions.Function1[_ >: A, _ <: Prop.Result], value: A): Prop.Result =
      test.invoke(value)
  }

  def checkFormula[Action, State, Response](actions: List[Action], initial: State, step: (Action, State) => Option[Step[State, Response]])(
      formula: Formula[Info[Action, State, Response]]
  ): Prop.Result = {
    val problem = CheckKt.check[Action, State, Response, Prop.Result, Prop.Result](
      new ScalacheckStepResultManager(),
      formula.asInstanceOf[Formula[Info[_ <: Action, _ <: State, _ <: Response]]],
      actions.asJava,
      initial,
      (action, current) => step(action, current).orNull,
      new java.util.ArrayList()
    )
    Option(problem).flatMap(p => Option(p.getError)).getOrElse(Prop.Result(status = Prop.True))
  }

  def checkFormula[F[_]: Monad, Action, State, Response](
      actions: List[Action],
      initial: F[State],
      step: (Action, State) => F[Option[Step[State, Response]]]
  )(
      formula: Formula[Info[Action, State, Response]]
  ): F[Prop.Result] = initial.flatMap(checkFormula(new ScalacheckStepResultManager[Info[Action, State, Response]](), step, actions, _, formula))

  def checkFormula[F[_]: Monad, Action, State, Response](
      resultManager: ScalacheckStepResultManager[Info[Action, State, Response]],
      step: (Action, State) => F[Option[Step[State, Response]]],
      actions: List[Action],
      current: State,
      formula: Formula[Info[Action, State, Response]]
  ): F[Prop.Result] = Monad[F].tailRecM((actions, current, formula)) {
    case (Nil, _, formula) =>
      noNullResult(CheckKt.leftToProve(resultManager, formula)).asRight.pure
    case (action :: rest, current, formula) =>
      step(action, current).flatMap {
        case None =>
          noNullResult(CheckKt.leftToProve(resultManager, formula)).asRight.pure
        case Some(oneStepFurther) =>
          val progress = CheckKt.check(resultManager, formula, new Info(action, current, oneStepFurther.getState, oneStepFurther.getResponse))
          if (!resultManager.isOk(progress.getResult))
            noNullResult(progress.getResult).asRight.pure
          else
            (rest, oneStepFurther.getState, progress.getNext).asLeft.pure
      }
  }

  private def noNullResult(
      result: Prop.Result
  ): Prop.Result = if (result == null) Prop.Result(status = Prop.True) else result

}
