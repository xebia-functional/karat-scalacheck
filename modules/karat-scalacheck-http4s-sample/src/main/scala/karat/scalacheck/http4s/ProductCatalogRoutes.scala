package karat.scalacheck.http4s

import cats.data.OptionT
import cats.effect._
import cats.syntax.all._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

object ProductCatalogRoutes {

  def routes[F[_]: Concurrent](storage: ProductStorage[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    implicit val pcListEE: EntityEncoder[F, List[ProductCatalog]] = jsonEncoderOf
    implicit val pcEE: EntityEncoder[F, ProductCatalog] = jsonEncoderOf
    implicit val pcED: EntityDecoder[F, ProductCatalog] = jsonOf

    val routes: HttpRoutes[F] = HttpRoutes.of[F] {

      case GET -> Root / "products" =>
        storage.all.flatMap(Ok(_))

      case GET -> Root / "products" / id =>
        OptionT(storage.find(id)).foldF(NotFound())(Ok(_))

      case req @ POST -> Root / "products" =>
        for {
          product <- req.as[ProductCatalog]
          result <- storage.add(product)
          response <- result.fold(_ => BadRequest(), _ => Accepted())
        } yield response

      case req @ PUT -> Root / "products" =>
        for {
          product <- req.as[ProductCatalog]
          result <- storage.update(product)
          response <- result.fold(NotFound())(_ => Ok())
        } yield response

      case DELETE -> Root / "products" / id =>
        storage.delete(id).flatMap(_.fold(NotFound())(_ => NoContent()))
    }

    routes
  }
}
