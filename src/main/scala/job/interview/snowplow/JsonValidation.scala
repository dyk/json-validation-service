package job.interview.snowplow

import cats.data.Validated._
import cats.effect.Concurrent
import cats.implicits._
import io.circe.ParsingFailure
import io.circe.parser.parse
import job.interview.snowplow.JsonValidation.ValidationResults.ValidationResult
import job.interview.snowplow.domain.SchemaId
import job.interview.snowplow.repo.SchemaRepo
import job.interview.snowplow.validation.JsonSchemaValidator

trait JsonValidation[F[_]] {
  def validate(schemaId: SchemaId, doc: F[String]): F[ValidationResult]
}

object JsonValidation {

  object ValidationResults {
    sealed trait ValidationResult
    case object ValidJson extends ValidationResult
    case class InvalidJson(errorMsg: String) extends ValidationResult
    case object SchemaNotFound extends ValidationResult
  }

  import ValidationResults._

  def impl[F[+_]: Concurrent](repo: SchemaRepo[F]): JsonValidation[F] =
    new JsonValidation[F] {
      override def validate(
          schemaId: SchemaId,
          doc: F[String]
      ): F[ValidationResult] = {
        doc.map(parse).flatMap {
          case Left(ParsingFailure(errorMsg, _)) =>
            InvalidJson(errorMsg).pure[F]
          case Right(json) =>
            repo
              .get(schemaId)
              .map(_.map { schema =>
                JsonSchemaValidator.validate(schema, json) match {
                  case Valid(_)          => ValidJson
                  case Invalid(errorMsg) => InvalidJson(errorMsg)
                }
              }.getOrElse(SchemaNotFound))
        }
      }
    }
}
