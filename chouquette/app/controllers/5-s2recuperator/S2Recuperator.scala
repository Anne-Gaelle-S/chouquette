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
import play.api.http.{ Status => StatusCode }


case class Features(features: Seq[JsValue]) {
  def toStringSeq: Seq[String] =
    features.flatMap(
      _.validate(
          (__ \ "properties" \ "services" \ "download" \ "url").read[String])
        .asOpt)
}

object Features {
  implicit val featuresReads: Reads[Features] = Json.reads[Features]
}


class S2Recuperator(
  cc: ControllerComponents,
  ws: WSClient,
  baseUrl: String
)(
  implicit ec: ExecutionContext
) extends AbstractController(cc) {

  @Inject def this(
    cc: ControllerComponents,
    ws: WSClient,
    ec: ExecutionContext
  ) = this(cc, ws, "https://peps.cnes.fr")(ec)


  def recupere = Action.async(parse.json) { request =>
    request.body.validate[String].asOpt
      .map(pepsImageUrl(_)
        .map(_
          .map(res => Ok(Json.toJson(res)))
          .getOrElse(InternalServerError("Request to peps failed"))))
      .getOrElse(Future(BadRequest("Invalid json: required string")))
  }

  def pepsImageUrl(
      utm: String,
      startDateVal: String="2016-11-05",
      completionDateVal: String="2016-11-15"
  ): Future[Option[Seq[String]]] = {
    val url = (baseUrl + "/resto/api/collections/S2ST/search.json"
      + "?tileid=" + utm
      + "&startDate=" + startDateVal
      + "&completionDate=" + completionDateVal)
    ws.url(url)
      .get
      .map(validate)
  }

  def validate(response: WSResponse): Option[Seq[String]] =
    if (response.status == StatusCode.OK)
      (response.json \ "features").validate[Seq[JsValue]].asOpt
        .map(_.flatMap(_.validate(
          (__ \ "properties" \ "services" \ "download" \ "url").read[String])
            .asOpt))
    else None


  def recupereDate = Action(parse.json).async { request =>
    (for {
      utm <- (request.body \ "utm").validate[String]
      startDateVal <- (request.body \ "startDateVal").validate[String]
      completionDateVal <- (request.body \ "completionDateVal").validate[String]
    } yield (pepsImageUrl(utm, startDateVal, completionDateVal)))
      .map(_
        .map(_
          .map(res => Ok(Json.toJson(res)))
          .getOrElse(InternalServerError("Couldn't parse peps response"))))
      .getOrElse(Future(BadRequest("""Invalid json: required fields:
        |- "utm": string
        |- "startDateVal": string // "yyyy-mm-dd"
        |- "completionDateVal": string // "yyyy-mm-dd"""")))
  }

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
