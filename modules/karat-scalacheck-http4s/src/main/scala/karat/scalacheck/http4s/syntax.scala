package karat.scalacheck.http4s

import cats.effect.MonadCancel
import cats.syntax.all._
import karat.concrete.progression.{Info, Step}
import karat.scalacheck.Scalacheck
import karat.scalacheck.effect.syntax._
import org.http4s.{Request, Response}
import org.http4s.client.Client
import org.scalacheck.effect.PropF

object syntax {

  private type MonadCancelThrow[F[_]] = MonadCancel[F, Throwable]

  type Formula[F[_], Action] = Scalacheck.Formula[Info[Action, Client[F], Response[F]]]

  def checkFormula[F[_]: MonadCancelThrow, Action](
      actions: List[Action],
      client: Client[F],
      request: Action => Request[F]
  )(formula: Formula[F, Action]): PropF[F] =
    Scalacheck
      .checkFormula[F, Action, Client[F], Response[F]](
        actions,
        client.pure[F],
        (a: Action, c: Client[F]) => c.run(request(a)).use(r => new Step(c, r).some.pure[F])
      )(
        formula
      ).toPropF

}
