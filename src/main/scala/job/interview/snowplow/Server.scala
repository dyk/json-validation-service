package job.interview.snowplow

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.Stream
import job.interview.snowplow.repo.FileSystemSchemaRepo
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger
import pureconfig.ConfigSource

import java.nio.file.Paths
import pureconfig.generic.auto._

object Server {

  case class AppConfig(jsonSchemaRepoBaseDir: String)

  def stream[F[+_]: Async]: Stream[F, Nothing] = {
    for {
      config <- Stream(ConfigSource.default.load[AppConfig].toOption.get)
      repo = new FileSystemSchemaRepo(
        Paths.get(config.jsonSchemaRepoBaseDir)
      )
      jsonSchemasAlg = JsonSchemas.impl[F](repo)
      jsonValidationAlg = JsonValidation.impl[F](repo)

      httpApp = (
        Routes.schemaRoutes(jsonSchemasAlg) <+>
          Routes.validateRoutes(jsonValidationAlg)
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- Stream.resource(
        EmberServerBuilder
          .default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(finalHttpApp)
          .build >>
          Resource.eval(Async[F].never)
      )
    } yield exitCode
  }.drain

}
