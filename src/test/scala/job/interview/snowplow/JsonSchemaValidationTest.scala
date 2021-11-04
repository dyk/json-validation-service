package job.interview.snowplow

import com.github.fge.jsonschema.examples.Utils
import com.github.fge.jsonschema.main.JsonSchemaFactory
import munit.FunSuite

class JsonSchemaValidationTest extends FunSuite {



  test("hejka") {
    println("adfasdf")


    val fstabSchema = Utils.loadResource("/fstab.json")
    val good = Utils.loadResource("/fstab-good.json")
    val bad = Utils.loadResource("/fstab-bad.json")
    val bad2 = Utils.loadResource("/fstab-bad2.json")

    val factory = JsonSchemaFactory.byDefault

    val schema = factory.getJsonSchema(fstabSchema)



    val report1 = schema.validate(good)
    println(report1.isSuccess)
    println(report1)

    val report2 = schema.validate(bad)
    println(report2.isSuccess)
    println(report2)

    val report3 = schema.validate(bad2)
    println(report3.isSuccess)
    println(report3)

  }

}
