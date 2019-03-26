package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import scala.concurrent._
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws._ // pour faire les requetes http (le client http quoi)

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
      Json.parse(response.body).validate[Seq[Description]].asOpt.flatMap(_.headOption)
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


