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

package karat.scalacheck.http4s

import cats.effect.MonadCancel
import cats.syntax.all._
import karat.concrete.progression.{Info, Step}
import karat.scalacheck.Scalacheck
import karat.scalacheck.effect.syntax._
import org.http4s.{Request, Response}
import org.http4s.client.Client
import org.scalacheck.effect.PropF

object syntax {

  private type MonadCancelThrow[F[_]] = MonadCancel[F, Throwable]

  type Formula[F[_], Action] = Scalacheck.Formula[Info[Action, Client[F], Response[F]]]

  def checkFormula[F[_]: MonadCancelThrow, Action](
      actions: List[Action],
      client: Client[F],
      request: Action => Request[F]
  )(formula: Formula[F, Action]): PropF[F] =
    Scalacheck
      .checkFormula[F, Action, Client[F], Response[F]](
        actions,
        client.pure[F],
        (a: Action, c: Client[F]) => c.run(request(a)).use(r => new Step(c, r).some.pure[F])
      )(
        formula
      ).toPropF

}
