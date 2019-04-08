import org.scalatestplus.play._

import play.core.server.Server
import play.api.BuiltInComponents
import play.api.routing.sird._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers.{ GET => UnusedGET, _ }
import play.inject.guice.GuiceApplicationBuilder

import akka.actor.ActorSystem
import akka.stream.Materializer

import chouquette.controllers.S2Recuperator


class S2RecuperatorSpec extends PlaySpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  def action(components: BuiltInComponents): DefaultActionBuilder =
    components.defaultActionBuilder


  "S2Recuperator.recupere" should {

    "return images urls" in {

      Server.withRouterFromComponents() { comp =>
        {
          case GET(p"/resto/api/collections/S2ST/search.json"
              ? q"tileid=$utm"
              & q"startDate=$startDate"
              & q"completionDate=$completionDate")
              if (  utm == "myutmzone"
                 && startDate == "2016-11-05"
                 && completionDate == "2016-11-15") => action(comp) {
            Ok(Json.parse("""
              {
                "features": [
                  {
                    "properties": {
                      "services": {
                        "download": {
                          "url": "valid url"
                        }
                      }
                    }
                  },
                  {
                    "properties": "this won't be parsed"
                  },
                  {
                    "properties": {
                      "services": {
                        "download": {
                          "url": "other valid url"
                        }
                      }
                    }
                  }
                ]
              }
            """))
          }
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val future =
            new S2Recuperator(
              stubControllerComponents(),
              client,
              s"http://localhost:$port")
            .recupere
            .apply(FakeRequest[JsValue](
              "",
              "",
              Headers(),
              body = JsString("myutmzone")))

          contentAsJson(future) mustBe
            Json.arr(JsString("valid url"), JsString("other valid url"))
        }
      }

    }

  }


  "S2Recuperator.recupereDate" should {

    "return images urls for specific dates" in {

      Server.withRouterFromComponents() { comp =>
        {
          case GET(p"/resto/api/collections/S2ST/search.json"
              ? q"tileid=$utm"
              & q"startDate=$startDate"
              & q"completionDate=$completionDate")
              if (  utm == "myutmzone"
                 && startDate == "start"
                 && completionDate == "completion") => action(comp) {
            Ok(Json.parse("""
              {
                "features": [
                  {
                    "properties": {
                      "services": {
                        "download": {
                          "url": "toto"
                        }
                      }
                    }
                  }
                ]
              }
            """))
          }
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val future =
            new S2Recuperator(
              stubControllerComponents(),
              client,
              s"http://localhost:$port")
            .recupereDate
            .apply(FakeRequest[JsValue](
              "",
              "",
              Headers(),
              body = Json.obj(
                "utm" -> "myutmzone",
                "startDateVal" -> "start",
                "completionDateVal" -> "completion")))

          contentAsJson(future) mustBe
            Json.arr(JsString("toto"))
        }
      }

    }

  }

}
