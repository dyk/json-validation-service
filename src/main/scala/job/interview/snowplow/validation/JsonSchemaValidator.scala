package job.interview.snowplow.validation

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.main.JsonSchemaFactory
import io.circe.Json

object JsonSchemaValidator {

  private def toJackson(json: Json): JsonNode =
    JsonLoader.fromString(json.deepDropNullValues.noSpaces)

  def validate(schema: Json, doc: Json): Validated[String, Json] = {

    val report = JsonSchemaFactory.byDefault
      .getJsonSchema(toJackson(schema))
      .validate(toJackson(doc))

    if (report.isSuccess)
      Valid(doc)
    else
      Invalid(report.iterator().next().getMessage)
  }
}
