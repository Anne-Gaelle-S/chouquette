package controllers

import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}
import scala.util.matching.Regex

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

case class Coords(lat: Double, long: Double)
object Coords {
  implicit val coordsReads: Reads[Coords] = (
    (JsPath \ "lat").read[Double] and
    (JsPath \ "long").read[Double]
  )(Coords.apply _)
}


class UTMZonator @Inject() (
  cc: ControllerComponents,
  ws: WSClient // for the http request
)(
  implicit ec: ExecutionContext // for the http response
) extends AbstractController(cc) {

  def validatorJsValue(coord: String): JsValue = {
    return (Json.parse(coord));
  }

  def zonator = Action(parse.json) { result => {
    val res = result.body
      .validate[Seq[Coords]]
      .asOpt
      .map( coords => utmTransformator(coords) ) // Ok(x.toString)
      .map(_.onComplete{
        case Success(mgrsJson) => {
          println("SUCCESS: "+extractUTMmajoritaire(mgrsJson))
          Ok(extractUTMmajoritaire(mgrsJson))
        }
        case Failure(err) => throw new IllegalStateException("Future failed")
      })
      // .getOrElse(BadRequest("Mauvaise requete... "))

    Ok("Ca marche")
  }}

  def utmTransformator(coordonnees: Seq[Coords]): Future[Seq[JsValue]] = {
    // println(coordonnees)
    
    val utmsFutures: Seq[Future[JsValue]] = coordonnees
      .map( (coord) => {
        ws.url("https://api.opencagedata.com/geocode/v1/json?key=4e76f5429883420b92d7e90569089f7c&q="+coord.lat+"%2C"+coord.long+"&pretty=1")
          .withHttpHeaders("Accept" -> "application/json")
          .get()
          .map(result => {
            val json = Json.parse(result.body)
            val mgrs = (json \\ "MGRS").head
            // println( "Element: "+mgrs)
            if (mgrs == Nil) {
              throw new IllegalArgumentException("One of parameters is illegal.")
            } else {
              mgrs
            }
          })
      })

    val futureUTMs: Future[Seq[JsValue]] = Future.sequence(utmsFutures)
    return futureUTMs
  }

  def extractUTMmajoritaire(mgrsJson: Seq[JsValue]): String = {
    val utms = mgrsJson.map( mgrs => { // 31NEG3322907942 => 31N
        val text = mgrs.as[String] // Json.stringify(mgrs)
        val myRegex = "[a-zA-Z]"
        val mercator = (text.split(myRegex, 2)).toList.head // 31
        val firstLetter = (myRegex.r findFirstIn text).getOrElse(println) // N
        mercator+firstLetter // 31N
      })

    val startAcc = utms.distinct.map(utm => new Tuple2(utm, 0))
    var nbOccurences = utms.foldLeft(startAcc){ (acc, utm) => {
      var newAcc = acc.map(tuple => {
        var newTuple = tuple
        if(tuple._1 == utm) { 
          newTuple = new Tuple2(tuple._1, (tuple._2+1))
        }
        newTuple
      })
      newAcc
    }} // list of tuples ((31N, 2), (39Q, 1) ...)

    var zoneUTMmajoritaire = nbOccurences.maxBy(_._2)._1 // la zone UTM majoritaire, ex: 31N
    return zoneUTMmajoritaire
  }

}

// API Key : 4e76f5429883420b92d7e90569089f7c
