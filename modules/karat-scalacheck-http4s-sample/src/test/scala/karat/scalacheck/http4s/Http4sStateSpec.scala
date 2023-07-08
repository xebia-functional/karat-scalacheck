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

import cats.effect.IO
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.effect.PropF

class Http4sStateSpec extends CatsEffectSuite with ScalaCheckEffectSuite {

  implicit val pcArb: Arbitrary[ProductCatalog] = Arbitrary {
    for {
      id <- Gen.uuid.map(_.toString)
      name <- Gen.identifier
      quantity <- Gen.choose[Long](0, 1000)
    } yield ProductCatalog(id, name, quantity)
  }

  test("checkRight") {
    PropF.forAllNoShrinkF { (p: ProductCatalog) =>
      val storage: ProductStorage[IO] = ProductStorage.impl[IO].unsafeRunSync()
      val httpApp: HttpApp[IO] = ProductCatalogRoutes.routes[IO](storage).orNotFound
      val client: Client[IO] = Client.fromHttpApp(httpApp)
      val postRequest: Request[IO] = Request(method = Method.POST, uri = uri"/products").withEntity(p)
      val getRequest: Request[IO] = Request(method = Method.GET, uri = uri"/products")
      val statusResponse = for {
        status <- client.status(postRequest)
        response <- client.expect[List[ProductCatalog]](getRequest)
      } yield (status.code, response)
      assertIO(statusResponse, (202, List(p)))
    }
  }
}
