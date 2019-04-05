package chouquette.controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._

import play.core.server.Server
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder


import scala.concurrent._
import scala.concurrent.duration._

import akka.actor.ActorSystem

class ExtractorSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  import scala.concurrent.ExecutionContext.Implicits.global

  "Extractor" should {
    "return an extractedText" in {

      val client = new GuiceApplicationBuilder().injector.instanceOf[WSClient]

      val myFuture = new Extractor(stubControllerComponents(), client)
        .extract
        .apply(FakeRequest[JsValue](
          "",
          "",
          Headers(),
          body = JsString("Parking avant l'église de Parsac, l'espace devant étant trop réduit. (D/A) Descendez le chemin qui longe l'église vers le Sud. (1) En bas, au carrefour, prenez en face jusqu'à la bifurcation Y. Prenez alors à droite la route qui mène au bord de l'étang et longer celui-ci à main gauche. (2) Obliquez à droite après l'étang et continuez tout droit. Longez un étang sur la droite. (3) Au croisement, virez à droite puis après avoir passé le ruisseau de la Barbanne, prenez à gauche et passez devant le lieu-dit Maison Neuve. (4) Au carrefour en T, tournez à gauche sur la route puis suivez le chemin qui va tourner à droite le long d'un bois.")))

      contentAsJson(myFuture) mustBe
        Json.arr(JsString("Parsac"), JsString("Barbanne"))

    }
  }

}
