package job.interview.snowplow.validation

import cats.data.Validated.Valid
import job.interview.snowplow.{jsonFromClasspath => json}
import munit.FunSuite

class JsonSchemaValidatorSpec extends FunSuite {

  import JsonSchemaValidator.validate

  test("should accept valid") {
    val doc = json("/json/fstab-good.json")
    assertEquals(
      validate(schema = json("/schemas/fstab.json"), doc = doc),
      Valid(doc)
    )
  }

  test("should accept valid 2") {
    val doc = json("/json/config.json")
    assertEquals(
      validate(schema = json("/schemas/config-schema.json"), doc = doc),
      Valid(json("/json/bob.json"))
    )
  }

  test("should report invalid 1") {
    assertEquals(
      validate(
        schema = json("/schemas/fstab.json"),
        doc = json("/json/fstab-bad.json")
      ),
      Valid(json("/json/bob.json"))
    )
  }

  test("should report invalid 2") {
    assertEquals(
      validate(
        schema = json("/schemas/fstab.json"),
        doc = json("/json/fstab-bad2.json")
      ),
      Valid(json("/json/bob.json"))
    )
  }

  test("com.github.fge.jsonschema bug?") {
    assertEquals(
      validate(
        schema = json("/json/bob.json"),
        doc = json("/json/config.json")
      ),
      Valid(json("/json/bob.json"))
    )
  }

}
