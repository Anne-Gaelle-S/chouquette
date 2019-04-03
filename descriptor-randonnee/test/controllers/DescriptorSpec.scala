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

class DescriptorSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {
  import scala.concurrent.ExecutionContext.Implicits.global

  "Descriptor" should { 
    "return a description" in {

      Server.withRouterFromComponents() { components =>
        import Results._
        import components.{ defaultActionBuilder => Action }
        {
          case GET(p"/description") => Action {
            Ok(Json.arr(Json.obj(
              "description" -> "une très longue description")))
          } 
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val myFuture =  new Descriptor(stubControllerComponents(), client)
              .requestMe(502272)
              .apply(FakeRequest()) 

          val result = Await.result(myFuture, 10 seconds)
          val res  = result.body.consumeData(new play.api.libs.concurrent.MaterializerProvider(ActorSystem()).get)
          res.map{
            case x => x.decodeString("UTF-8") mustBe Seq("une très longue description")
          }
        }
      }
    } 
  }
}



