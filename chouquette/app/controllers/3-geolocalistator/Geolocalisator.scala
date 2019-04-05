package chouquette.controllers

import javax.inject.Inject

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import play.api._
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.json._
import play.api.libs.functional.syntax._ // Combinator syntax
import play.api.http.HttpEntity

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString

case class GeolocalisedText( var places: List[JsValue] )

object GeolocalisedText {
  implicit val GeolocalisedTextReads = Json.reads[GeolocalisedText]
  implicit val GeolocalisedTextWrites = Json.writes[GeolocalisedText]
}

class Geolocalisator @Inject() (
  cc: ControllerComponents,
  mat: Materializer
) extends AbstractController(cc) {

  val url1 = "https://dbpedia.org/sparql?default-graph-uri=http%3A%2F%2Fdbpedia.org&query=select+%3Fplace+%3Fpoint+where+%7B+%0D%0A%3Fplace+rdf%3Atype+dbo%3APlace+.+%0D%0A%3Fplace+rdfs%3Alabel+%3FnomPlace+filter%28str%28%3FnomPlace%29%3D%22"
  val url2 = "%22%29+.+%0D%0A%3Fplace+georss%3Apoint+%3Fpoint%0D%0A%7D+limit+1&format=application%2Fsparql-results%2Bjson&CXML_redir_for_subjs=121&CXML_redir_for_hrefs=&timeout=30000&debug=on&run=+Run+Query+"

  def getURL(url: String) = scala.io.Source.fromURL(url).mkString

  def geolocalise = Action(parse.json) {res => {

    var resultat:GeolocalisedText = new GeolocalisedText(List())

    println("POST REQUEST")

    val optionOfString = res.body.validate[Seq[String]].asOpt

    optionOfString.map( x => x.foreach { x =>
      val coordinates = (
        ((Json.parse(getURL(url1+x+url2)) \ "results" \ "bindings").get).head \ "point" \ "value"
      ).get

      val coordinatesString = (coordinates.toString.substring(
        1,
        coordinates.toString.size-1
      ).split(" "))

      val coordinatesJson: JsValue = Json.obj(
        "long" -> coordinatesString(1).toDouble,
        "lat" -> coordinatesString(0).toDouble
      )
      resultat.places = resultat.places ::: List(coordinatesJson)

    }).getOrElse(BadRequest("Mauvaise requete... "))

    // println(Json.toJson(resultat).getClass)
        Ok(Json.toJson(resultat))
  }}
}
