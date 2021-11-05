package job.interview.snowplow

import cats.effect.{Async, Resource}
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.Stream
import job.interview.snowplow.repo.FileSystemSchemaRepo
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import org.http4s.server.middleware.Logger

import java.nio.file.Paths

object Server {

  def stream[F[+_]: Async]: Stream[F, Nothing] = {
    for {
      client <- Stream.resource(EmberClientBuilder.default[F].build)
      helloWorldAlg = HelloWorld.impl[F]
      jokeAlg = Jokes.impl[F](client)
      repo = new FileSystemSchemaRepo(
        Paths.get(System.getProperty("java.io.tmpdir"))
      )

      jsonSchemasAlg = JsonSchemas.impl[F](repo)
      jsonValidationAlg = JsonValidation.impl[F](repo)

      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract a segments not checked
      // in the underlying routes.
      httpApp = (
        Routes.helloWorldRoutes[F](helloWorldAlg) <+>
          Routes.jokeRoutes[F](jokeAlg) <+>
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
