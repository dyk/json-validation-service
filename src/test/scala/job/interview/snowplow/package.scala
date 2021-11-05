package job.interview

import fs2.Chunk
import io.circe.Json
import io.circe.parser.parse
import org.apache.commons.io.FileUtils

import java.io.File
import java.nio.charset.StandardCharsets

package object snowplow {
  def toByteStream(s: String) =
    fs2.Stream.chunk(Chunk.array(s.getBytes("UTF-8")))
  def json(s: String) = parse(s).toOption.get
  def jsonFromClasspath(resource: String): Json =
    parse(
      FileUtils.readFileToString(
        new File(getClass.getResource(resource).getPath),
        StandardCharsets.UTF_8
      )
    ).toOption.get

}
