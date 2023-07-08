package karat.scalacheck.effect

import cats.MonadThrow
import cats.syntax.all.*
import org.scalacheck.Prop
import org.scalacheck.effect.PropF

object instances:

  extension [F[_]: MonadThrow](effectProp: F[Prop.Result])
    def toPropF: PropF[F] =
      PropF.effectOfPropFToPropF(
        effectProp.map { result =>
          PropF.Result(result.status, result.args, result.collected, result.labels)
        }
      )
