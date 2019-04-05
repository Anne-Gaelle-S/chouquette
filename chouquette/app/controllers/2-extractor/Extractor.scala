package chouquette.controllers

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
  cc: ControllerComponents,
  ws: WSClient // for the http request
)(
  implicit ec: ExecutionContext // for the http response
) extends AbstractController(cc) {

  def validator(response: WSResponse): Option[JsValue] = {
    if(response.status == play.api.http.Status.OK)
      Some( Json.toJson(response.json \\ "@surfaceForm") )
    else None
  }

  def extract = Action.async(parse.json) { request =>
    request.body.validate[String].asOpt
      .map(extractFromText(_)
        .map(_
          .map(Ok(_))
          .getOrElse(NotFound("Not found"))))
      .getOrElse(Future(BadRequest("Invalid json: required string")))
  }

  def extractFromText(textToAnnotate: String): Future[Option[JsValue]] =
    ws.url("http://icc.pau.eisti.fr/rest/annotate?text="+textToAnnotate)
      .withHttpHeaders("Accept" -> "application/json")
      .get()
      .map(validator(_))

}
