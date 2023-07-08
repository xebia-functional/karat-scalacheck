package karat.scalacheck.effect

import cats.MonadThrow
import cats.syntax.all._
import org.scalacheck.Prop
import org.scalacheck.effect.PropF

object instances {

  implicit class PropFOps[F[_]](effectProp: F[Prop.Result]) {
    def toPropF(implicit F: MonadThrow[F]): PropF[F] =
      PropF.effectOfPropFToPropF(
        effectProp.map { result =>
          PropF.Result(result.status, result.args, result.collected, result.labels)
        }
      )
  }

}
