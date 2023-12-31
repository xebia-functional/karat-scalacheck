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

package karat.scalacheck.effect

import cats.MonadThrow
import cats.syntax.all.*
import org.scalacheck.Prop
import org.scalacheck.effect.PropF

object syntax:

  extension [F[_]: MonadThrow](effectProp: F[Prop.Result])
    def toPropF: PropF[F] =
      PropF.effectOfPropFToPropF(
        effectProp.map { result =>
          PropF.Result(result.status, result.args, result.collected, result.labels)
        }
      )
