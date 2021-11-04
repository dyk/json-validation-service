package job.interview.snowplow

import cats.Applicative
import io.circe.Json
import cats.implicits._
import job.interview.snowplow.domain.SchemaId
import job.interview.snowplow.repo.SchemaRepo

trait JsonSchemas[F[_]]{
  def store(schemaId: SchemaId, schema: F[String]): F[JsonSchemas.StoringResults.StoringResult]
  def get(schemaId: SchemaId): F[Option[Json]]
}


object JsonSchemas {

  object StoringResults {
    sealed trait StoringResult
    case class Success(schemaId: SchemaId) extends StoringResult
    //case class AlreadyExists(schemaId: SchemaId) extends StoringResult
    case class Invalid(schemaId: SchemaId) extends StoringResult
  }

  import StoringResults._

  def impl[F[+_]: Applicative](repo: SchemaRepo[F]): JsonSchemas[F] = new JsonSchemas[F]{
    override def get(schemaId: SchemaId): F[Option[Json]] = repo.get(schemaId)
    override def store(schemaId: SchemaId, schema: F[String]): F[StoringResult] = Success(schemaId).pure[F]
  }
}
