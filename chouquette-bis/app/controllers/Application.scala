package controllers

import play.api._
import play.api.mvc._
import javax.inject.Inject

class Application @Inject() (
  cc: ControllerComponents
) extends AbstractController(cc) {

  def index() = Action { 
    Ok("Ca marche")
  }
}
