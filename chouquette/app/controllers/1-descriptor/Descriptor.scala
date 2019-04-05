package chouquette.controllers

import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent._
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws._ // pour faire les requetes http (le client http quoi)

// Service 1 :
// Récupérer un texte de randonnée via l’API ​ https://api.camptocamp.org/outings/​
// ou son extraction ​ https://choucas.blqn.fr/data/outing/

case class Description(description: String)

object Description {
  implicit val descriptionRead = Json.reads[Description]
}

@Singleton
class Descriptor @Inject()(
  cc: ControllerComponents,
  ws: WSClient
)(
  implicit ec: ExecutionContext
) extends AbstractController(cc) {

  def validator(response: WSResponse): Option[Description] = {
    if(response.status == play.api.http.Status.OK)
      response.json.validate[Seq[Description]].asOpt.flatMap(_.headOption)
    else None
  }

  def requestMe(id: Int) = Action.async {
    ws.url("https://choucas.blqn.fr/data/outing/"+id)
      .get
      .map(validator)
      .map {
        case Some(description) => Ok(Json.toJson(description.description))
        case None => NotFound
      }
  }

}
