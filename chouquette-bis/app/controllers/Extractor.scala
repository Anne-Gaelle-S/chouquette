package controllers

import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import play.api._
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.json._
import play.api.http.HttpEntity

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

// import models._

case class ExtractedText( text: String )

object ExtractedText {
  implicit val extractedTextReads = Json.reads[ExtractedText]
  implicit val extractedTextWrites = Json.writes[ExtractedText]
}

class Extractor @Inject() (
  ws: WSClient // for the http request
)(
  implicit ec: ExecutionContext // for the http response
) extends Controller {

  def validator(response: WSResponse): Option[Seq[JsValue]] = {
    if(response.status == play.api.http.Status.OK) {
      Some( (response.json \\ "Resources") )
    }
    else None
  } 

  def extract(textToAnnotate: String) = Action.async {
    ws
      .url("http://icc.pau.eisti.fr/rest/annotate?text="+textToAnnotate)
      .withHeaders("Accept" -> "application/json")
      .get()
      .map(validator)
      .map {
        case Some(extractedText) => {
          println(extractedText)
          Ok( extractedText.flatMap( semanticText => (semanticText \\ "@URI") ).toString )
          // Ok( extractedText.flatMap(
          //    semanticText => (
          //      ((semanticText \\ "@URI").toString),((semanticText \\ "@types").toString)
          //      ) 
          //   ).toString )
          // Ok( extractedText.flatMap( semanticText => (semanticText) ).toString ) 
          //Il faudrait renvoyer une liste de couple, peut-être ?
        }
        case None => NotFound("Not found")
      }
  }

}