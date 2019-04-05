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

case class ImagesURLS( var urls: List[String])

case class ResRequeteJson(var json: JsValue)

object ImagesURLS {
  implicit val ImagesURLSReads = Json.reads[ImagesURLS]
  implicit val ImagesURLSWrites = Json.writes[ImagesURLS]
}

object ResRequeteJson {
  implicit val ResRequeteJsonReads = Json.reads[ResRequeteJson]
  implicit val ResRequeteJsonWrites = Json.writes[ResRequeteJson]
}

class S2Recuperator @Inject() (
  cc: ControllerComponents,
  mat: Materializer
) extends AbstractController(cc) {


  def getURL(url: String) = scala.io.Source.fromURL(url).mkString

  def recupere = Action(parse.json) {res => {

    val startDateVal = "2016-11-05"
    val completionDateVal = "2016-11-15"


    var resultat:ImagesURLS = new ImagesURLS(List())

    println("POST REQUEST")

    val utmList = res.body.validate[Seq[String]].asOpt

    utmList.map{ x =>

      val utm = x(0)

      val url = "https://peps.cnes.fr/resto/api/collections/S2ST/search.json?tileid="+utm+"&startDate="+startDateVal+"&completionDate="+completionDateVal

      val jsValue:JsValue = Json.toJson("")
      var rep: ResRequeteJson = new ResRequeteJson(jsValue)
      rep.json = (Json.parse(getURL(url)) \ "features").get

      val repJson = rep.json.as[List[JsValue]]

      repJson.foreach{x =>

        val urlImage = (x \ "properties" \ "services" \ "download" \ "url").get

        resultat.urls = resultat.urls ::: List(
          urlImage.toString.substring(
            1,
            urlImage.toString.size-1
          )
        )

      }
    // println(resultat)
    }.getOrElse(BadRequest("Mauvaise requete... "))

    Ok(Json.toJson(resultat))

  }}


def recupereDate = Action(parse.json) {res => {

    var resultat:ImagesURLS = new ImagesURLS(List())

    println("POST REQUEST")

    val utmList = res.body.validate[JsValue].asOpt

    utmList.map{ x =>

      val utm = ((x \ "utm").get)
        .toString.substring(
            1,
            ((x \ "utm").get).toString.size-1
        )

      val startDateVal = ((x \ "startDateVal").get)
        .toString.substring(
            1,
            ((x \ "startDateVal").get).toString.size-1
        )

      val completionDateVal = ((x \ "completionDateVal").get)
        .toString.substring(
            1,
            ((x \ "completionDateVal").get).toString.size-1
        )


      val url = "https://peps.cnes.fr/resto/api/collections/S2ST/search.json?tileid="+utm+"&startDate="+startDateVal+"&completionDate="+completionDateVal

      val jsValue:JsValue = Json.toJson("")
      var rep: ResRequeteJson = new ResRequeteJson(jsValue)
      rep.json = (Json.parse(getURL(url)) \ "features").get

      val repJson = rep.json.as[List[JsValue]]

      repJson.foreach{x =>

        val urlImage = (x \ "properties" \ "services" \ "download" \ "url").get

        resultat.urls = resultat.urls ::: List(
          urlImage.toString.substring(
            1,
            urlImage.toString.size-1
          )
        )

      }
    // println(resultat)
    }.getOrElse(BadRequest("Mauvaise requete... "))

    Ok(Json.toJson(resultat))

  }}
}


/*
* Exemple :
*   mgrs = 31TCJ
*   startDateVal = 2015-12-01
*   completionDateVal = 2015-12-31
* Requête : https://peps.cnes.fr/resto/api/collections/S2/search.json?tileid=mgrs&startDate=startDateVal&completionDate=completionDateVal
* Exemple : https://peps.cnes.fr/resto/api/collections/S2ST/search.json?tileid=31TEN&startDate=2016-11-05&completionDate=2016-11-15

* Itérer dans features (tableau de json)
* A renvoyer : properties/services/download/url
*/
