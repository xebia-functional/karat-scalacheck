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

import io.circe.Codec
import io.circe.generic.semiauto._

import scala.util.control.NoStackTrace

case class ProductCatalog(id: String, name: String, quantity: Long)
object ProductCatalog {
  implicit val productCatalogCodec: Codec[ProductCatalog] = deriveCodec
}

case object ProductAlreadyExistError extends RuntimeException("Product already exist") with NoStackTrace
