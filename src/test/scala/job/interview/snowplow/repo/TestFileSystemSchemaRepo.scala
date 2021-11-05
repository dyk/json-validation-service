package job.interview.snowplow.repo

import cats.effect.IO
import java.nio.file.Path

class TestFileSystemSchemaRepo(val baseDir: Path) extends FileSystemSchemaRepo[IO](baseDir)