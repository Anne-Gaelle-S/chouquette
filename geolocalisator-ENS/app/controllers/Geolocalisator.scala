package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent._
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws._ // pour faire les requetes http (le client http quoi)


// Service 3 : 
// Geolocaliser des ENS via le web service ​ https://dbpedia.org/sparql

case class Geolocalisator(description: String)

object Geolocalisator {
  implicit val descriptionRead = Json.reads[Description]
}

@Singleton
class Descriptor @Inject()(
  cc: ControllerComponents,
  ws: WSClient
)(
  implicit ec: ExecutionContext
) extends AbstractController(cc) {

  def localisatorENS() = ???

}


