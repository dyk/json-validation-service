package job.interview.snowplow

import cats.effect.Concurrent
import io.circe._
import io.circe.parser._
import cats.implicits._
import job.interview.snowplow.domain.SchemaId
import job.interview.snowplow.repo.SchemaRepo

trait JsonSchemas[F[_]] {
  def store(
      schemaId: SchemaId,
      schema: F[String]
  ): F[JsonSchemas.StoringResults.StoringResult]
  def get(schemaId: SchemaId): F[Option[Json]]
}

object JsonSchemas {

  object StoringResults {
    sealed trait StoringResult
    case class Success(schemaId: SchemaId) extends StoringResult
    case class Invalid(schemaId: SchemaId, errorMsg: String)
        extends StoringResult
  }

  import StoringResults._

  def impl[F[+_]: Concurrent](repo: SchemaRepo[F]): JsonSchemas[F] =
    new JsonSchemas[F] {
      override def get(schemaId: SchemaId): F[Option[Json]] = repo.get(schemaId)
      override def store(
          schemaId: SchemaId,
          schema: F[String]
      ): F[StoringResult] = {
        schema.map(parse).flatMap {
          case Left(ParsingFailure(errorMsg, _)) =>
            Invalid(schemaId, errorMsg).pure[F]
          case Right(json) =>
            repo.store(schemaId, json).map { _ => Success(schemaId) }
        }
      }
    }
}
