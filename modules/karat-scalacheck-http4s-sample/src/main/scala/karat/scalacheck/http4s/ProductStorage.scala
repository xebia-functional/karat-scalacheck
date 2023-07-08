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

import cats.effect.{Async, Ref}
import cats.syntax.all._

trait ProductStorage[F[_]] {
  def all: F[List[ProductCatalog]]
  def find(id: String): F[Option[ProductCatalog]]
  def add(p: ProductCatalog): F[Either[ProductAlreadyExistError.type, Unit]]
  def update(p: ProductCatalog): F[Option[Unit]]
  def delete(id: String): F[Option[Unit]]
}

object ProductStorage {
  def impl[F[_]: Async]: F[ProductStorage[F]] = Ref.of[F, Map[String, ProductCatalog]](Map.empty).map(apply[F])

  private def apply[F[_]: Async](ref: Ref[F, Map[String, ProductCatalog]]): ProductStorage[F] = new ProductStorage[F] {
    override def all: F[List[ProductCatalog]] = ref.get.map(_.values.toList)

    override def find(id: String): F[Option[ProductCatalog]] = ref.get.map(_.get(id))

    override def add(p: ProductCatalog): F[Either[ProductAlreadyExistError.type, Unit]] =
      ref.modify { map =>
        if (map.contains(p.id)) (map, Left(ProductAlreadyExistError))
        else (map.+(p.id -> p), Right((): Unit))
      }

    override def update(p: ProductCatalog): F[Option[Unit]] =
      ref.modify { map =>
        if (map.contains(p.id)) (map.+(p.id -> p), Some((): Unit))
        else (map, None)
      }

    override def delete(id: String): F[Option[Unit]] =
      ref.modify { map =>
        if (map.contains(id)) (map.-(id), Some((): Unit))
        else (map, None)
      }
  }
}
