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

package karat.scalacheck.http4s.test

import cats.data.Kleisli
import cats.effect.IO
import cats.syntax.all._
import karat.scalacheck.ArbModel
import karat.scalacheck.syntax._
import karat.scalacheck.http4s._
import karat.scalacheck.http4s.syntax._
import munit.{CatsEffectSuite, ScalaCheckEffectSuite}
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.circe.CirceEntityEncoder._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.effect.PropF

class Http4sSMSpec extends CatsEffectSuite with ScalaCheckEffectSuite {

  val idGen: Gen[String] = Gen.choose(1, 3).map(_.toString)

  implicit val pcArb: Arbitrary[ProductCatalog] = Arbitrary {
    for {
      id <- idGen
      name <- Gen.identifier
      quantity <- Gen.choose[Long](0, 1000)
    } yield ProductCatalog(id, name, quantity)
  }

  sealed abstract class Action(val request: Request[IO]) extends Product with Serializable {
    def isCreate: Boolean = this match {
      case Action.CreateProduct(_) => true
      case _ => false
    }
    def getId: Option[String] = this match {
      case Action.GetProducts => none
      case Action.GetProduct(id) => id.some
      case Action.CreateProduct(p) => p.id.some
      case Action.UpdateProduct(p) => p.id.some
      case Action.DeleteProduct(id) => id.some
    }
  }
  object Action {
    case object GetProducts extends Action(Request(method = Method.GET, uri = uri"/products"))
    case class GetProduct(id: String) extends Action(Request(method = Method.GET, uri = uri"/products" / id))
    case class CreateProduct(p: ProductCatalog) extends Action(Request(method = Method.POST, uri = uri"/products").withEntity(p))
    case class UpdateProduct(p: ProductCatalog) extends Action(Request(method = Method.PUT, uri = uri"/products").withEntity(p))
    case class DeleteProduct(id: String) extends Action(Request(method = Method.DELETE, uri = uri"/products" / id))
  }

  def idBasedOnState(state: Set[String]): Gen[String] =
    if (state.isEmpty) idGen
    else Gen.frequency((1, idGen), (3, Gen.oneOf(state)))

  val model: ArbModel[Set[String], Action] = new ArbModel[Set[String], Action] {
    def initial: Set[String] = Set.empty

    def updateProductGen(state: Set[String]): Gen[Action] =
      for {
        pc <- Arbitrary.arbitrary[ProductCatalog]
        id <- idBasedOnState(state)
      } yield Action.UpdateProduct(pc.copy(id = id))

    def nexts(state: Set[String]): Arbitrary[Option[Action]] = Arbitrary(
      Gen
        .oneOf[Action](
          Gen.const(Action.GetProducts),
          idBasedOnState(state).map(Action.GetProduct(_)),
          Arbitrary.arbitrary[ProductCatalog].map(Action.CreateProduct(_)),
          updateProductGen(state),
          idBasedOnState(state).map(Action.DeleteProduct(_))
        ).map(Some(_))
    )

    def step(state: Set[String], action: Action): Set[String] = action match {
      case Action.CreateProduct(p) => state.+(p.id)
      case Action.DeleteProduct(id) => state.-(id)
      case _ => state
    }
  }

  val statusFormula: Formula[IO, Action] = {
    val validCodes: Action => Set[Int] = {
      case Action.GetProducts => Set(200)
      case Action.GetProduct(_) => Set(200, 404)
      case Action.CreateProduct(_) => Set(202, 400)
      case Action.UpdateProduct(_) => Set(200, 404)
      case Action.DeleteProduct(_) => Set(204, 404)
    }
    always(should(item => validCodes(item.getAction).contains(item.getResponse.status.code)))
  }

  val getFormula: Formula[IO, Action] =
    always {
      implies(
        should(_.getAction.isCreate),
        remember { current =>
          val rememberedId: Option[String] = current.getAction.getId
          afterwards(
            implies(
              should {
                _.getAction match {
                  case Action.GetProduct(id) => rememberedId.contains(id)
                  case _ => false
                }
              },
              should(_.getResponse.status.code == 200)
            )
          )
        }
      )
    }

  val getFormulaEventually: Formula[IO, Action] =
    always {
      implies(
        should(_.getAction.isCreate),
        remember { current =>
          val rememberedId: Option[String] = current.getAction.getId
          eventually(
            implies(
              should {
                _.getAction match {
                  case Action.GetProduct(id) => rememberedId.contains(id)
                  case _ => false
                }
              },
              should(_.getResponse.status.code == 200)
            )
          )
        }
      )
    }

  val getFormulaImmediate: Formula[IO, Action] =
    always {
      implies(
        should(_.getAction.isCreate),
        remember { current =>
          val rememberedId: Option[String] = current.getAction.getId
          next(
            implies(
              should {
                _.getAction match {
                  case Action.GetProduct(id) => rememberedId.contains(id)
                  case _ => false
                }
              },
              should(_.getResponse.status.code == 200)
            )
          )
        }
      )
    }

  test("Verify status") {
    PropF
      .forAllF(model.gen) { actions =>
        val storage: ProductStorage[IO] = ProductStorage.impl[IO].unsafeRunSync()
        val httpApp: HttpApp[IO] =
          Kleisli(a => ProductCatalogRoutes.routes[IO](storage).run(a).getOrRaise(new RuntimeException("Route not found!")))
        val client: Client[IO] = Client.fromHttpApp(httpApp)
        checkFormula[IO, Action](actions, client, _.request)(statusFormula)
      }.check()
  }

  test("Return created product") {
    PropF
      .forAllF(model.gen) { actions =>
        val storage: ProductStorage[IO] = ProductStorage.impl[IO].unsafeRunSync()
        val httpApp: HttpApp[IO] =
          Kleisli(a => ProductCatalogRoutes.routes[IO](storage).run(a).getOrRaise(new RuntimeException("Route not found!")))
        val client: Client[IO] = Client.fromHttpApp(httpApp)
        checkFormula[IO, Action](actions, client, _.request)(getFormulaEventually)
      }.check()
  }

  test("Eventually return created product") {
    PropF
      .forAllF(model.gen) { actions =>
        val storage: ProductStorage[IO] = ProductStorage.impl[IO].unsafeRunSync()
        val httpApp: HttpApp[IO] =
          Kleisli(a => ProductCatalogRoutes.routes[IO](storage).run(a).getOrRaise(new RuntimeException("Route not found!")))
        val client: Client[IO] = Client.fromHttpApp(httpApp)
        checkFormula[IO, Action](actions, client, _.request)(getFormulaImmediate)
      }.check()
  }
}
