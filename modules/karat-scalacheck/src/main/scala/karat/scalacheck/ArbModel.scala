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

import org.scalacheck.{Arbitrary, Gen}

trait ArbModel[State, Action] {
  def initial: State
  def nexts(state: State): Arbitrary[Option[Action]]
  def step(state: State, action: Action): State

  def gen: Gen[List[Action]] = Gen.sized { size =>
    (1 to size)
      .foldLeft(Gen.const[(List[Action], State, Boolean)]((Nil, initial, false))) { case (genState, _) =>
        genState.flatMap {
          case (l, state, true) => Gen.const((l, state, true))
          case (l, state, false) =>
            nexts(state).arbitrary
              .map(_.fold((l, state, true))(a => (l :+ a, step(state, a), false)))
        }
      }.map(_._1)
  }
}

trait StatelessArbModel[Action] extends ArbModel[Unit, Action] {
  val initial: Unit = {}
  def nexts(state: Unit): Arbitrary[Option[Action]] = nexts()
  def step(state: Unit, action: Action): Unit = {}
  def nexts(): Arbitrary[Option[Action]]
}
