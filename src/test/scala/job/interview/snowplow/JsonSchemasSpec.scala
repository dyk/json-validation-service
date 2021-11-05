package job.interview.snowplow

import cats.effect.IO
import fs2.Chunk
import io.circe._
import job.interview.snowplow.domain.SchemaId
import job.interview.snowplow.repo.FileSystemSchemaRepo
import munit.CatsEffectSuite
import org.http4s._
import org.http4s.circe.jsonOf
import org.http4s.implicits._
import io.circe.parser._
import org.apache.commons.io.FileUtils

import java.nio.file.{Files, Path}


class JsonSchemasSpec extends CatsEffectSuite {

  implicit def jsonDecoder[A : Decoder]: EntityDecoder[IO, A] = jsonOf[IO, A]

  class TestFileSystemSchemaRepo(val baseDir: Path) extends FileSystemSchemaRepo[IO](baseDir)

  case class SchemasFixture(repo: TestFileSystemSchemaRepo, jsonSchemas: JsonSchemas[IO]) {
    def post(body: String, schemaId: SchemaId) = {
      val postRequest = Request[IO](method = Method.POST, uri = uri"/schema".addPath(schemaId.name), body = toByteStream(body))
      Routes.schemaRoutes(jsonSchemas).orNotFound(postRequest)
    }
    def get(schemaId: SchemaId) = {
      val getRequest = Request[IO](method = Method.GET, uri = uri"/schema".addPath(schemaId.name))
      Routes.schemaRoutes(jsonSchemas).orNotFound(getRequest)
    }

  }

  def json(s: String) = parse(s).toOption.get

  val jsonSchemas = FunFixture[SchemasFixture](
    setup = { test =>
      val repo = new TestFileSystemSchemaRepo(Files.createTempDirectory(s"${test.name}_"))
      SchemasFixture(repo, JsonSchemas.impl[IO](repo))
    },
    teardown = { f =>
      FileUtils.deleteDirectory(f.repo.baseDir.toFile)
    }
  )

  def toByteStream(s: String) = fs2.Stream.chunk(Chunk.array(s.getBytes("UTF-8")))


  jsonSchemas.test("should upload schema") { jst =>
    val res = jst.post("""{"name":"Alice"}""", SchemaId("test1"))
    assertIO(res.map(_.status), Status.Ok)
    assertIO(res.flatMap(_.as[Json]), json(
    """{
        "action": "uploadSchema",
        "id": "test1",
        "status": "success"
      }"""))
  }

  jsonSchemas.test("should get schema") { jst =>
    val res = jst.get(SchemaId("test3"))
    assertIO(res.map(_.status), Status.NotFound)
  }

  jsonSchemas.test("should get uploded schema") { jst =>

    val schemaId = SchemaId("get-after-post")
    assertIO(jst.post("""{"name":"Bob"}""", schemaId).map(_.status), Status.Ok)

    val res = jst.get(schemaId)
    assertIO(res.map(_.status), Status.Ok)
    assertIO(res.flatMap(_.as[Json]), json(
      """{
        "action": "uploadSchema",
        "id": "test1",
        "status": "success"
      }"""))
  }

  jsonSchemas.test("should report error on invalid json") { jst =>

    val schemaId = SchemaId("invalid-json")
    val res = jst.post("""{{invalid json""", schemaId)
    assertIO(res.map(_.status), Status.Ok)
    assertIO(res.flatMap(_.as[Json]), json(
      """{
        "action": "uploadSchema",
        "id": "test1",
        "status": "success"
      }"""))
  }


}

