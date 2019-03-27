package controllers

// import javax.inject.Inject
// import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.api._
import play.api.mvc._
import play.api.libs.json._

import models.Product

// object Extractor {
//   implicit val locationName = Extractor("")
// }

class Extractor extends Controller {

  def semanticText(text: String) = ???

  def retreive(text: String) = ???
 
  // JSON response
  def extract(text: String) = Action {
    val text = Product(1, "GOOD")
    Ok(Json.toJson(text))
  }
  // Action.async {
  //   semanticText(text).retreive()
  // }

}