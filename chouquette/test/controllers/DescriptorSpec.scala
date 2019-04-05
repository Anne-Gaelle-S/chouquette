package chouquette.controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._

import play.core.server.Server
import play.api.routing.sird._
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor.ActorSystem

class DescriptorSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  import scala.concurrent.ExecutionContext.Implicits.global

  "Descriptor" should {
    "return a description" in {

      val client = new GuiceApplicationBuilder().injector.instanceOf[WSClient]

      val myFuture = new Descriptor(stubControllerComponents(), client)
        .requestMe(502272)
        .apply(FakeRequest())

      contentAsJson(myFuture) mustBe JsString("Parking avant l'église de Parsac, l'espace devant étant trop réduit. (D/A) Descendez le chemin qui longe l'église vers le Sud. (1) En bas, au carrefour, prenez en face jusqu'à la bifurcation Y. Prenez alors à droite la route qui mène au bord de l'étang et longer celui-ci à main gauche. (2) Obliquez à droite après l'étang et continuez tout droit. Longez un étang sur la droite. (3) Au croisement, virez à droite puis après avoir passé le ruisseau de la Barbanne, prenez à gauche et passez devant le lieu-dit Maison Neuve. (4) Au carrefour en T, tournez à gauche sur la route puis suivez le chemin qui va tourner à droite le long d'un bois. (5) En face d'une construction, prendre à droite un chemin herbeux. Passez devant le lieu-dit Piron et rejoignez une route. Traversez le lieu-dit Berlière en ignorant la route à droite. (6) À la dernière maison, prenez à gauche pour rejoindre un bosquet et le longer par la droite. (7) Au carrefour de quatre routes, prendre la 2ème à droite (Est). Laissez à droite le départ vers le hameau de Musset puis à gauche vers celui de Puynormond. Rejoignez la D130. Restez à gauche sur la D130, passez devant le petit étang au niveau de Lestage. (8) Juste après ce lieu-dit, dans le virage, prenez à droite sur quelques mètres puis à gauche à la bifurcation en Y. Prenez le chemin, longez le bois et continuez jusqu'au deuxième bosquet. Continuez à droite et traversez celui-ci puis descendez jusqu'à la route sur le chemin du Château des Laurets. (9) Arrivé sur la route, obliquez à droite pour rejoindre le bas de Parsac. (1) Tournez à droite et remontez au parking avant l'église (D/A).Points de passage : D/A : km 0 - alt. 67m - Parking avant l'église 1 : km 0.27 - alt. 46m - Carrefour de la boucle 2 : km 0.86 - alt. 33m - À droite après l'étang 3 : km 1.88 - alt. 31m - Deuxième étang 4 : km 2.54 - alt. 38m - Carrefour en T 5 : km 3.23 - alt. 41m - À droite vers le Piron 6 : km 4.03 - alt. 50m - À gauche à la dernière maison 7 : km 4.62 - alt. 66m - Musset 8 : km 5.79 - alt. 79m - À droite après Lestage 9 : km 6.83 - alt. 59m - À droite, sur la route D/A : km 8.17 - alt. 67m - Parking avant l'église")

    }
  }
}
