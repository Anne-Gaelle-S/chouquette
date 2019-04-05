package chouquette.controllers

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
    println("POST REQUEST")
    // val optionOfCoords = res.body.validate[Seq[Coords]].asOpt
    // println(optionOfCoords.map(x => Ok(x.toString)))
    result.body
      .validate[Seq[Coords]]
      .asOpt
      .map( coords => utmTransformator(coords) ) // Ok(x.toString)
      .getOrElse(BadRequest("Mauvaise requete... "))

    // Ok("Ca marche")
  }}

  def utmTransformator(coordonnees: Seq[Coords]) = {
    println("-------------------------------------------")
    println(coordonnees)
    println("~~~~~~~~~")
    coordonnees.map( (coord) => {
      println(coord.lat+"  -   "+coord.long)
      ws.url("https://api.opencagedata.com/geocode/v1/json?key=4e76f5429883420b92d7e90569089f7c&q="+coord.lat+"%2C"+coord.long+"&pretty=1")
        .withHttpHeaders("Accept" -> "application/json")
        .get()
        .map( (res) => {
          println("RESULTTAAAAT:")
          println(res)
          val json = Json.parse(res.body)
          println( json \\ "MGRS")
          // res
        })
    })

    Ok(coordonnees.toString)
  }
}

// API Key : 4e76f5429883420b92d7e90569089f7c
