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
