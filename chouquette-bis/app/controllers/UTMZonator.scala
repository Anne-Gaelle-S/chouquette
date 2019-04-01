package controllers

import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import play.api._
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.json._// JSON library
import play.api.libs.json.Reads._ // Custom validation helpers
import play.api.libs.functional.syntax._ // Combinator syntax
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

case class Coords(long: Double, lat: Double)
object Coords {
  implicit val coordsReads: Reads[Coords] = (
    (JsPath \ "long").read[Double] and
    (JsPath \ "lat").read[Double]
  )(Coords.apply _)
}

class UTMZonator @Inject() (
  ws: WSClient // for the http request
)(
  implicit ec: ExecutionContext // for the http response
) extends Controller {

  def validatorJsValue(coord: String): JsValue = {
    return (Json.parse(coord));
  }

  def zonatora = Action(parse.json) {res => {
    println("POST REQUEST")
    val optionOfCoords = res.body.validate[Seq[Coords]].asOpt
    // println(optionOfCoords.map(x => Ok(x.toString)))
    optionOfCoords.map( x => Ok(x.toString) ).getOrElse(BadRequest("Mauvaise requete... "))
  }}

  def zonator(coordonnees: String) = Action.async {
    println("-------------------------------------------");
    // val coordJson = validatorJsValue(coordonnees);
    // println(coordJson);
    
    // val latReads: Reads[Coords] = (JsPath \ "lat").read[Coords]
    // val latResult = coordJson.validate[Seq[Coords]]
    // latResult match {
    //   case s: JsSuccess[Coords] => println("Lat: " + s.get)
    //   case e: JsError => println("Errors: " + JsError.toJson(e).toString() +"\n"+e)
    // }

    println("-------------------------------------------");
    println("-------------------------------------------");

    ws
      .url("https://www.latlong.net/c/?lat=35.000000&long=600.000000")
      .withHeaders("Accept" -> "application/json")
      .get()
      .map((res) => {
        // println("------------------------------------------------------------------");
        // println("RESULTTAAAAT:");
        // println(res);
        // println(res.body);
        Ok(coordonnees);
      }) 
  }

}

// API Key : 4e76f5429883420b92d7e90569089f7c