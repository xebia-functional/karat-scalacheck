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

import cats.MonadError
import cats.effect.IO
import cats.syntax.all._
import karat.concrete.FormulaKt.{always, predicate}
import karat.concrete.progression.{Info, Step}
import karat.scalacheck.Scalacheck.{checkFormula, Formula}
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.scalacheck.Prop._
import org.scalacheck.effect.PropF
import org.scalacheck.{Arbitrary, Gen, Prop}

class TestCounter extends CatsEffectSuite with ScalaCheckEffectSuite {

  sealed trait Action extends Product with Serializable
  object Action {
    case object Increment extends Action
    case object Read extends Action
  }

  val model: ArbModel[Unit, Action] = new StatelessArbModel[Action] {
    override def nexts(): Arbitrary[Option[Action]] = Arbitrary(Gen.some(Gen.oneOf(Action.Increment, Action.Read)))
  }

  val gen: Gen[Action] = Gen.oneOf(Action.Increment, Action.Read)

  def right(action: Action, state: Int): Option[Step[Int, Int]] = Some(action match {
    case Action.Increment => new Step(state + 1, 0)
    case Action.Read => new Step(state, state)
  })

  def wrong(action: Action, state: Int): Option[Step[Int, Int]] = Some(action match {
    case Action.Increment =>
      new Step(state + 1, 0)
    case Action.Read =>
      new Step(state, if (state == 10) -1 else state)
  })

  def error(action: Action, state: Int): Option[Step[Int, Int]] = Some(action match {
    case Action.Increment => new Step(state + 1, 0)
    case Action.Read =>
      new Step(
        state, {
          if (state == 10) throw new RuntimeException("ERROR!")
          state + 1
        }
      )
  })

  def formula: Formula[Info[Action, Int, Int]] =
    always {
      predicate { (item: Info[Action, Int, Int]) =>
        // TODO: provide better accessors
        val status = item.getAction match {
          case Action.Read if item.getResponse >= 0 => Prop.True
          case Action.Read => Prop.False
          case _ => Prop.True
        }
        Prop.Result(status)
      }
    }

  val initialState: Int = 0
  val stepAction: (Action, Int) => Option[Step[Int, Int]] = right
  val initialFormula: Formula[Info[Action, Int, Int]] = formula

  // TODO
  // Maybe this is provided by scalacheck-effect
  implicit class PropFOps[F[_]](effectProp: F[Prop.Result]) {
    def toPropF(implicit F: MonadError[F, Throwable]): PropF[F] =
      PropF.effectOfPropFToPropF(
        effectProp.map { result =>
          PropF.Result(result.status, result.args, result.collected, result.labels)
        }
      )
  }

  test("checkRight") {
    forAll(model.gen) { actions =>
      val result = checkFormula(actions, initialState, stepAction)(initialFormula)
      assert(result.success)
    }
  }

  test("checkRightIO") {
    PropF
      .forAllF(model.gen) { actions =>
        checkFormula[IO, Action, Int, Int](
          actions,
          IO(initialState),
          (action: Action, state: Int) => IO(stepAction(action, state))
        )(initialFormula).toPropF
      }.check()
  }

  // property("checkWrong") = forAll(model.gen) { actions =>
  //   checkFormula(actions, initialState, wrong)(initialFormula)
  // }

  // property("checkThrow") = forAll(model.gen) { actions =>
  //   checkFormula(actions, initialState, error)(initialFormula)
  // }

}
