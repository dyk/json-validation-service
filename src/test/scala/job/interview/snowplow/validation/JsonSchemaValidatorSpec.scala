package job.interview.snowplow.validation

import io.circe.Json
import io.circe.parser._
import munit.FunSuite
import org.apache.commons.io.FileUtils

import java.io.File
import java.nio.charset.StandardCharsets


class JsonSchemaValidatorSpec extends FunSuite {

  import JsonSchemaValidator.validate

  def json(resource: String): Json =
    parse(
      FileUtils.readFileToString(
        new File(getClass.getResource(resource).getPath),
        StandardCharsets.UTF_8))
      .toOption.get

  test("should accept valid") {
    validate(schema = json("/schemas/fstab.json"), doc = json("/json/fstab-good.json"))
  }

  test("should accept valid 2") {
    validate(schema = json("/schemas/config-schema.json"), doc = json("/json/config.json"))
  }

  test("should report invalid 1") {
    validate(schema = json("/schemas/fstab.json"), doc = json("/json/fstab-bad.json"))
  }

  test("should report invalid 2") {
    validate(schema = json("/schemas/fstab.json"), doc = json("/json/fstab-bad2.json"))
  }

}
