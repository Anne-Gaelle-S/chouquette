package chouquette.controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._

import play.core.server.Server
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._
import akka.stream.Materializer
import play.api.libs.concurrent.MaterializerProvider

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor.ActorSystem


class GeolocalisatorSpec extends PlaySpec with Injecting with GuiceOneAppPerTest {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit lazy val materializer: Materializer = app.materializer
  implicit lazy val components = inject[ControllerComponents]

  "Geolocalisator" should {
    "not return 404" when {
      "I go to the route /geolocalise" in {
        val result = route(app, FakeRequest(POST, "/geolocalise"))
        status(result.get) must not be NOT_FOUND
      }
    }
  }

  "Geolocalise" should {
    "return Ok with a body containing a json with lat and long" in {

      val geolocalisateur = new Geolocalisator(components, mat = materializer)

      val request = FakeRequest("POST", "/geolocalise").withJsonBody(Json.parse(s"""["Pau","Toulouse","Lyon"]"""));

      val apiResult = call(geolocalisateur.geolocalise, request)

      Thread.sleep( 5000 )

      status(apiResult) mustEqual OK

      val resultBody = contentAsJson(apiResult)

      resultBody mustEqual Json.parse(
        s"""{
          "places": [
            {
              "long": -0.37,
              "lat": 43.3
            },
            {
              "long": 1.444,
              "lat": 43.6045
            },
            {
              "long": 4.84,
              "lat": 45.76
            }
          ]
        }"""
      )
    }
  }
}
