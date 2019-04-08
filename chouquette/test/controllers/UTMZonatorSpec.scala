import org.scalatestplus.play._

import play.core.server.Server
import play.api.BuiltInComponents
import play.api.routing.sird._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers.{ GET => UnusedGET, _ }

import chouquette.controllers.UTMZonator


class UTMZonatorSpec extends PlaySpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  def action(components: BuiltInComponents): DefaultActionBuilder =
    components.defaultActionBuilder


  "UTMZonator" should {

    "zone" in {

      Server.withRouterFromComponents() { comp =>
        {
          case GET(p"/data/outing/502272") => action(comp) {
            Ok(Json.parse("""
              [
                {
                  "description": "it works",
                  "unused field": 123
                },
                {
                  "description": "unused description"
                }
              ]
            """))
          }
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val future =
            new UTMZonator(
              stubControllerComponents(),
              client,
              s"http://localhost:$port")
            .zonator
            .apply(FakeRequest[JsValue](
              "",
              "",
              Headers(),
              body = Json.parse("""
                [
                  {
                    "lat": 12.3,
                    "long": 4.56
                  },
                  {
                    "lat": 7.89,
                    "long": 0.12
                  }
                ]
              """)))

          contentAsString(future) mustBe "Ca marche"
        }
      }

    }

  }

}
