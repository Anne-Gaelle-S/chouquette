package chouquette.controllers

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


case class UTMZone(text: String)

object UTMZone {
  implicit val extractedTextReads = Json.reads[UTMZone]
  implicit val extractedTextWrites = Json.writes[UTMZone]
}

case class Coords(lat: Double, long: Double)
object Coords {
  implicit val coordsReads: Reads[Coords] = (
    (JsPath \ "lat").read[Double] and
    (JsPath \ "long").read[Double]
  )(Coords.apply _)
}


class UTMZonator(
  cc: ControllerComponents,
  ws: WSClient, // for the http request
  baseUrl: String
)(
  implicit ec: ExecutionContext // for the http response
) extends AbstractController(cc) {

  @Inject def this(
    cc: ControllerComponents,
    ws: WSClient,
    ec: ExecutionContext
  ) = this(cc, ws, "https://api.opencagedata.com")(ec)

  def validatorJsValue(coord: String): JsValue = {
    return (Json.parse(coord));
  }

  def zonator = Action.async(parse.json) { result => {
    result.body
      .validate[Seq[Coords]]
      .asOpt
      .map( coords => utmTransformator(coords) ) // Ok(x.toString)
      .map( allMgrs => {
        allMgrs.map( mgrs => Ok(extractUTMmajoritaire(mgrs) ))
      })
      .getOrElse(Future(BadRequest("Mauvaise requete... ")))
  }}

  def utmTransformator(coordonnees: Seq[Coords]): Future[Seq[JsValue]] = {
    val utmsFutures: Seq[Future[JsValue]] = coordonnees
      .map( (coord) => {
        ws.url(baseUrl + "/geocode/v1/json"
            + "?key=4e76f5429883420b92d7e90569089f7c"
            + "&q=" + coord.lat+"%2C"+coord.long
            + "&pretty=1")
          .withHttpHeaders("Accept" -> "application/json")
          .get()
          .map(result => {
            val json = Json.parse(result.body)
            val mgrs = (json \\ "MGRS").head
            if (Json.stringify(mgrs) == Nil) {
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
    val utms = mgrsJson.map( mgrs => { // 31NDB3322907942 => 31NDB
        val mgrsTotal = mgrs.as[String] 
        val mgrsCut = 
         (mgrsTotal(0).toString + // 3
          mgrsTotal(1).toString + // 1
          mgrsTotal(2).toString + // N
          mgrsTotal(3).toString + // D
          mgrsTotal(4).toString)  // B
        mgrsCut
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
