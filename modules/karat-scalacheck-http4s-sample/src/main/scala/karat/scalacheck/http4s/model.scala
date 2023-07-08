package karat.scalacheck.http4s

import io.circe.Codec
import io.circe.generic.semiauto._

import scala.util.control.NoStackTrace

case class ProductCatalog(id: String, name: String, quantity: Long)
object ProductCatalog {
  implicit val productCatalogCodec: Codec[ProductCatalog] = deriveCodec
}

case object ProductAlreadyExistError extends RuntimeException("Product already exist") with NoStackTrace
