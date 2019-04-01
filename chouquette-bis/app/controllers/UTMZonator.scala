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

case class UTMZone( text: String )

object UTMZone {
  implicit val extractedTextReads = Json.reads[ExtractedText]
  implicit val extractedTextWrites = Json.writes[ExtractedText]
}

class UTMZonator @Inject() (
  ws: WSClient // for the http request
)(
  implicit ec: ExecutionContext // for the http response
) extends Controller {

  def zonator(coordonnees: String) = Action { //.async {
    Ok(coordonnees);
    // ws
    //   .url("http://icc.pau.eisti.fr/rest/annotate?text="+textToAnnotate)
    //   .withHeaders("Accept" -> "application/json")
    //   .get()
    //   .map(validator)
    //   .map {
    //     case Some(extractedText) => {
    //       println(extractedText)
    //       Ok( extractedText.flatMap( semanticText => (semanticText \\ "@URI") ).toString )
    //     }
    //     case None => NotFound("Not found")
    //   }
  }

}