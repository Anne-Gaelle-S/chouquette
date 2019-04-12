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

          future.onComplete{
            case Success(res) => res mustBe Future("31PFP")
            case Failure(res) => res mustBe Future(BadRequest("Mauvaise requete... "))
          }
        }
      }
    }

    "found the major UTM zone from more data" in {
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
              .apply(FakeRequest[JsValue]("", "", Headers(),
                body = Json.parse("""
                  [
                    {"long": 2.2, "lat": 1.1},
                    {"long": 3.3, "lat": 5.5},
                    {"long": 15, "lat": 2},
                    {"long": 30, "lat": 20}
                  ]
                """)))

          future.onComplete{
            case Success(res) => res mustBe Future("31NDB")
            case Failure(res) => res mustBe Future(BadRequest("Mauvaise requete... "))
          }
        }
      }
    }

    "throw a error if not correct entry" in {
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
              .apply(FakeRequest[JsValue]("", "", Headers(),
                body = Json.parse("""
                  [{"lat": 5.5}]
                """)))

          future.onComplete{
            case Success(res) => res mustBe Future("<not completed>")
            case Failure(res) => res mustBe Future(BadRequest("Mauvaise requete... "))
          }
        }
      }
    }

  }

}
