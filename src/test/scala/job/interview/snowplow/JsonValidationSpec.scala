package job.interview.snowplow

import cats.effect.IO
import fs2.Chunk
import io.circe._
import io.circe.parser._
import job.interview.snowplow.domain.SchemaId
import job.interview.snowplow.repo.{FileSystemSchemaRepo, TestFileSystemSchemaRepo}
import munit.CatsEffectSuite
import org.apache.commons.io.FileUtils
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.implicits._

import java.nio.file.{Files, Path}

class JsonValidationSpec extends CatsEffectSuite {

  implicit def jsonDecoder[A : Decoder]: EntityDecoder[IO, A] = jsonOf[IO, A]

  case class ValidationFixture(repo: TestFileSystemSchemaRepo, jsonValidation: JsonValidation[IO]) {
    def post(body: String, schemaId: SchemaId) = {
      val postRequest = Request[IO](method = Method.POST, uri = uri"/validate".addPath(schemaId.name), body = toByteStream(body))
      Routes.validateRoutes(jsonValidation).orNotFound(postRequest)
    }
  }

  val jsonValidation = FunFixture[ValidationFixture](
    setup = { test =>
      val repo = new TestFileSystemSchemaRepo(Files.createTempDirectory(s"${test.name}_"))
      ValidationFixture(repo, JsonValidation.impl[IO](repo))
    },
    teardown = { f =>
      FileUtils.deleteDirectory(f.repo.baseDir.toFile)
    }
  )

  jsonValidation.test("should return not found when there is no schema") { jvt =>
    val res = jvt.post("""{"name":"Alice"}""", SchemaId("no-such-schema"))
    assertIO(res.map(_.status), Status.NotFound)
  }

  jsonValidation.test("should validate valid json") { jvt =>
    val res = (for {
      _ <- jvt.repo.store(SchemaId("config-schema"), jsonFromClasspath("/schemas/config-schema.json"))
      res <- jvt.post(jsonFromClasspath("/json/config.json").noSpaces, SchemaId("config-schema"))
    } yield res)

    assertIO(res.map(_.status), Status.NotFound)
        assertIO(res.flatMap(_.as[Json]), json("""{
            "action": "validateDocument",
            "id": "config-schema",
            "status": "success"
          }"""))
  }

  jsonValidation.test("should validate invalid json") { jvt =>
    val res = (for {
      _ <- jvt.repo.store(SchemaId("config-schema"), jsonFromClasspath("/schemas/config-schema.json"))
      res <- jvt.post("""{"name":"Alice"}""", SchemaId("config-schema"))
    } yield res)

    assertIO(res.map(_.status), Status.NotFound)
    assertIO(res.flatMap(_.as[Json]), json("""{
            "action": "validateDocument",
            "id": "config-schema",
            "status": "error",
            "message": "object has missing required properties ([\"destination\",\"source\"])"
          }"""))
  }
}