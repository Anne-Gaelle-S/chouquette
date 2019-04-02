package service67.services

import scala.concurrent._

import javax.inject._
import play.api.libs.ws._

import service67.MyExecutionContext
import service67.controllers.Downloadable


@Singleton
class Downloader @Inject()(
  ws: WSClient
)(
  implicit ec: MyExecutionContext
) extends Downloadable {

  def downloadImage(url: String)
                   (implicit ec: ExecutionContext): Future[String] = ???

}
