package job.interview.snowplow.repo

import munit.CatsEffectSuite
import cats.effect.IO
import io.circe.Json
import job.interview.snowplow.domain.SchemaId
import org.apache.commons.io.FileUtils

import java.io.File
import java.nio.file.{Files, Path}

class FileSystemSchemaRepoSpec extends CatsEffectSuite {

  val tmpDir = FunFixture[Path](
    setup = { test =>
      Files.createTempDirectory(s"${test.name}_")
    },
    teardown = { dir =>
      FileUtils.deleteDirectory(dir.toFile)
    }
  )

  tmpDir.test("repo should store and get schema") { baseDir =>
    val repo = new FileSystemSchemaRepo[IO](baseDir)
    val schemaId = SchemaId("test1")
    val json = for {
      _ <- repo.store(schemaId, Json.fromString("test-json1"))
      fetched <- repo.get(schemaId)
    } yield fetched

    assertIO(json, Option(Json.fromString("test-json1")))
  }

  tmpDir.test("repo should not get invalid schema") { baseDir =>

    FileUtils.copyFile(
      new File(getClass.getResource("/schemas/sample.pdf").getPath),
      baseDir.resolve("invalid-schema").toFile)

    val repo = new FileSystemSchemaRepo[IO](baseDir)

    assertIO(repo.get(SchemaId("invalid-schema")), None)
  }

  tmpDir.test("repo should override schema when exists") { baseDir =>

    val repo = new FileSystemSchemaRepo[IO](baseDir)
    val schemaId = SchemaId("test1")
    val json = for {
      _ <- repo.store(schemaId, Json.fromString("test-json1"))
      _ <- repo.store(schemaId, Json.fromString("test-json2"))
      fetched <- repo.get(schemaId)
    } yield fetched

    assertIO(json, Option(Json.fromString("test-json2")))
  }


}
