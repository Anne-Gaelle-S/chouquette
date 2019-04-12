import org.scalatestplus.play._

import play.core.server.Server
import play.api.BuiltInComponents
import play.api.routing.sird._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers.{ GET => UnusedGET, _ }

import scala.concurrent.Await
import scala.concurrent.Future
import scala.util.{Success, Failure}
import scala.concurrent.duration._

import chouquette.controllers.UTMZonator


class UTMZonatorSpec extends PlaySpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  def action(components: BuiltInComponents): DefaultActionBuilder =
    components.defaultActionBuilder


  "UTMZonator" should {

    "found the major UTM zone from little data" in {
      Server.withRouterFromComponents() { comp =>
        {
          // latLong 1
          case GET(p"/geocode/v1/json" ? q"q=$latLong")
              if (latLong == "12.3,4.56") => action(comp) {
            Ok(Json.parse("""
              {
                "some field": [
                  {
                    "MGRS": "abcdefg"
                  },
                  {
                    "MGRS": "won't be used"
                  }
                ]
              }
            """))
          }
          // latLong 2
          case GET(p"/geocode/v1/json" ? q"q=$latLong")
              if (latLong == "7.89,0.12") => action(comp) {
            Ok(Json.parse("""
              {
                "MGRS": "hijklmn"
              }
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
            .apply(FakeRequest[JsValue]("", "", Headers(),
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

          status(future) mustBe OK
          contentAsString(future) mustBe "abcde"
        }
      }
    }

    "found the major UTM zone from more data" in {
      Server.withRouterFromComponents() { comp =>
        {
          case GET(p"/geocode/v1/json") => action(comp) {
            Ok(Json.parse("""
              {
                "MGRS": "moar than 5 chars"
              }
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
            .apply(FakeRequest[JsValue]("", "", Headers(),
              body = Json.parse("""
                [
                  {"long": 2.2, "lat": 1.1},
                  {"long": 3.3, "lat": 5.5},
                  {"long": 15, "lat": 2},
                  {"long": 30, "lat": 20}
                ]
              """)))

          status(future) mustBe OK
          contentAsString(future) mustBe "kaboum"
        }
      }
    }

    "throw a error if not correct entry" in {
        WsTestClient.withClient { client =>
          val future =
            new UTMZonator(
              stubControllerComponents(),
              client,
              "whatever, this won't be called")
            .zonator
            .apply(FakeRequest[JsValue]("", "", Headers(),
              body = Json.parse("""
                [{"lat": 5.5}]
              """)))

          status(future) mustBe BAD_REQUEST
          contentAsString(future) mustBe "Mauvaise requete... "
      }
    }

  }

}
