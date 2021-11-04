package job.interview.snowplow.repo

import munit.CatsEffectSuite
import cats.effect.IO
import io.circe.Json
import job.interview.snowplow.domain.SchemaId
import org.apache.commons.io.FileUtils

import java.nio.file.{Files, Path}

class FileSystemSchemaRepoSpec extends CatsEffectSuite {

  val tmpDir = FunFixture[Path](
    setup = { test =>
      Files.createTempDirectory(s"${test.name}_")
    },
    teardown = { dir =>
      FileUtils.deleteDirectory(dir.toFile);
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

}
