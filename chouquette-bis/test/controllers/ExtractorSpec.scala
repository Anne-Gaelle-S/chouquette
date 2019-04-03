package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._

import play.core.server.Server
import play.api.routing.sird._
import play.api.mvc._
import play.api.libs.ws._ 
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers.stubControllerComponents

import scala.concurrent._
import scala.concurrent.duration._

import akka.actor.ActorSystem

class ExtractorSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  import scala.concurrent.ExecutionContext.Implicits.global

  "Extractor" should { 
    "return an extractedText" in {

      Server.withRouterFromComponents() { components =>
        import Results._
        import components.{ defaultActionBuilder => Action }
        {
          case GET(p"/extractedText") => Action {
            Ok(Json.arr(Json.obj(
              "extractedText" -> "un extractedText")))
          } 
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val myFuture =  new Extractor(stubControllerComponents(), client)
              .extract("Parking avant l'église de Parsac, l'espace devant étant trop réduit. (D/A) Descendez le chemin qui longe l'église vers le Sud. (1) En bas, au carrefour, prenez en face jusqu'à la bifurcation Y. Prenez alors à droite la route qui mène au bord de l'étang et longer celui-ci à main gauche. (2) Obliquez à droite après l'étang et continuez tout droit. Longez un étang sur la droite. (3) Au croisement, virez à droite puis après avoir passé le ruisseau de la Barbanne, prenez à gauche et passez devant le lieu-dit Maison Neuve. (4) Au carrefour en T, tournez à gauche sur la route puis suivez le chemin qui va tourner à droite le long d'un bois.")
              .apply(FakeRequest()) 

          val result = Await.result(myFuture, 10 seconds)
          val res  = result.body.consumeData(new play.api.libs.concurrent.MaterializerProvider(ActorSystem()).get)
          res.map{
            case x => x.decodeString("UTF-8") mustBe Seq("un extractedText")
          }
        }
      }

    } 
  }

}



