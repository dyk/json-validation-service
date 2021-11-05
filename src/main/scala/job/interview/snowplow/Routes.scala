package job.interview.snowplow

import cats.effect._
import org.http4s._
import cats.implicits._
import io.circe._
import job.interview.snowplow.domain.SchemaId
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._

object Routes {

  object responses {
    private def response(
        action: String,
        schemaId: SchemaId,
        status: String,
        message: Option[String] = None
    ) = {
      val fields =
        List(
          ("action", Json.fromString(action)),
          ("id", Json.fromString(schemaId.name)),
          ("status", Json.fromString(status))
        ) ++ message.toList.map(m => ("message", Json.fromString(m)))

      Json.obj(fields: _*)
    }

    object schema {
      def uploaded(schemaId: SchemaId) =
        response(
          action = "uploadSchema",
          status = "success",
          schemaId = schemaId
        )
      def error(schemaId: SchemaId, errorMsg: String) = response(
        action = "uploadSchema",
        status = "error",
        schemaId = schemaId,
        message = Some(errorMsg)
      )
    }

    object validation {
      def ok(schemaId: SchemaId) = response(
        action = "validateDocument",
        status = "success",
        schemaId = schemaId
      )
      def error(schemaId: SchemaId, errorMsg: String) = response(
        action = "validateDocument",
        status = "error",
        schemaId = schemaId,
        message = Some(errorMsg)
      )
    }

  }

  def schemaRoutes[F[_]: Concurrent](S: JsonSchemas[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    import JsonSchemas._

    HttpRoutes.of[F] {
      case GET -> Root / "schema" / schemaIdParam =>
        S.get(SchemaId(schemaIdParam)).flatMap {
          case None    => NotFound()
          case Some(s) => Ok(s)
        }

      case req @ POST -> Root / "schema" / schemaIdParam =>
        for {
          result <- S.store(SchemaId(schemaIdParam), req.as[String])
          resp <- result match {
            case StoringResults.Success(schemaId) =>
              Ok(responses.schema.uploaded(schemaId))
            case StoringResults.Invalid(schemaId, errorMsg) =>
              Ok(responses.schema.error(schemaId, errorMsg))
          }
        } yield resp
    }
  }

  def validateRoutes[F[_]: Concurrent](V: JsonValidation[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    import JsonValidation._

    HttpRoutes.of[F] { case req @ POST -> Root / "validate" / schemaIdParam =>
      val schemaId = SchemaId(schemaIdParam)
      for {
        result <- V.validate(schemaId, req.as[String])
        resp <- result match {
          case ValidationResults.ValidJson =>
            Ok(responses.validation.ok(schemaId))
          case ValidationResults.InvalidJson(errorMsg) =>
            Ok(responses.validation.error(schemaId, errorMsg))
          case ValidationResults.SchemaNotFound => NotFound()
        }
      } yield resp
    }
  }

}
