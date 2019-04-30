package chouquette.controllers

import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent._
import play.api.http.{ Status => StatusCode }
import play.api.libs.json._
import play.api.libs.ws._ // pour faire les requetes http (le client http quoi)

// Service 1 :
// Récupérer un texte de randonnée via l’API ​:
//   https://api.camptocamp.org/outings/​
// ou son extraction : https://choucas.blqn.fr/data/outing/

case class Description(description: String)

object Description {
  implicit val descriptionRead = Json.reads[Description]
}

@Singleton
class Descriptor(
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
  ) = this(cc, ws, "https://choucas.blqn.fr")(ec)

  def validator(response: WSResponse): Option[Description] =
    if (response.status == StatusCode.OK)
      response.json.validate[Seq[Description]].asOpt.flatMap(_.headOption)
    else None

  def requestMe(id: Int) = Action.async {
    ws.url(s"$baseUrl/data/outing/"+id)
      .get
      .map(validator)
      .map {
        case Some(description) => Ok(Json.toJson(description.description))
        case None => NotFound
      }
  }

}
