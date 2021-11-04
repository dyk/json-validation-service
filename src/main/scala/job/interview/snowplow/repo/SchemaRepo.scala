package job.interview.snowplow.repo

import cats.effect._
import io.circe._
import io.circe.parser._
import job.interview.snowplow.domain.SchemaId

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

trait SchemaRepo[F[_]] {
  def get(schemaId: SchemaId): F[Option[Json]]
  def store(schemaId: SchemaId, schema: Json): F[Unit]
}


class FileSystemSchemaRepo[F[_]: Sync](baseDir: Path) extends SchemaRepo[F] {

  override def get(schemaId: SchemaId): F[Option[Json]] = {
    val path = baseDir.resolve(schemaId.name)
    if (path.toFile.exists())
      Sync[F].blocking {
        parse(Files.readString(path)).toOption
      }
    else Sync[F].pure(None)
  }

  override def store(schemaId: SchemaId, schema: Json): F[Unit] = {
    val path = baseDir.resolve(schemaId.name)
    Files.deleteIfExists(path)
    Sync[F].blocking {
      Files.write(path, schema.noSpaces.getBytes(StandardCharsets.UTF_8))
    }
  }
}
