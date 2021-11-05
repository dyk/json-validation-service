package job.interview.snowplow.validation

import job.interview.snowplow.{jsonFromClasspath => json}
import munit.FunSuite

class JsonSchemaValidatorSpec extends FunSuite {

  import JsonSchemaValidator.validate

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
