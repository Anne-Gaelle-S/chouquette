import org.scalatestplus.play._

import play.core.server.Server
import play.api.BuiltInComponents
import play.api.routing.sird._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers.{ GET => UnusedGET, _ }

import chouquette.controllers.Extractor


class ExtractorSpec extends PlaySpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  def action(components: BuiltInComponents): DefaultActionBuilder =
    components.defaultActionBuilder


  "Extractor" should {

    "return an extractedText" in {

      Server.withRouterFromComponents() { comp =>
        {
          case GET(p"/rest/annotate" ? q"text=$text")
              if text == "awesometext" => action(comp) {
            Ok(Json.parse("""
              {
                "someField": {
                  "@surfaceForm": "toto"
                },
                "@surfaceForm": "titi"
              }
            """))
          }
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val future =
            new Extractor(
              stubControllerComponents(),
              client,
              s"http://localhost:$port")
            .extract
            .apply(FakeRequest[JsValue](
              "",
              "",
              Headers(),
              body = JsString("awesometext")))

              contentAsJson(future) mustBe
                Json.arr(JsString("toto"), JsString("titi"))
            }
      }

    }

  }

}
