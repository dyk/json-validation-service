package job.interview.snowplow

import cats.effect._
import org.http4s._
import cats.implicits._
import io.circe._
import job.interview.snowplow.domain.SchemaId
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._


object Routes {

  def schemaRoutes[F[_]: Concurrent](S: JsonSchemas[F]) : HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    import JsonSchemas._

    //implicit def jsonDecoder[A : Decoder]: EntityDecoder[F, A] = jsonOf[F, A]

    HttpRoutes.of[F] {
      case GET -> Root / "schema" / schemaId =>
        S.get(SchemaId(schemaId)).flatMap {
            case None => NotFound()
            case Some(s) => Ok(s)
          }

      case req @ POST -> Root / "schema" / schemaId =>
        for {
          result <- S.store(SchemaId(schemaId), req.as[String])
          resp <- result match {
            case StoringResults.Success(schemaId) => Ok(Json.obj(
              ("action", Json.fromString("uploadSchema")),
              ("id", Json.fromString(schemaId.name))
            ))
            case StoringResults.Invalid(_) => Ok("error")
          }
        } yield resp
    }
  }

  def jokeRoutes[F[_]: Sync](J: Jokes[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "joke" =>
        for {
          joke <- J.get
          resp <- Ok(joke)
        } yield resp
    }
  }

  def helloWorldRoutes[F[_]: Sync](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "hello" / name =>
        for {
          greeting <- H.hello(HelloWorld.Name(name))
          resp <- Ok(greeting)
        } yield resp
    }
  }
}