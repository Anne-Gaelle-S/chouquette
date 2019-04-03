package service67.services

import scala.concurrent._

import javax.inject._

import service67.MyExecutionContext
import service67.controllers.Tileable


@Singleton
class Tiler @Inject()()(
  implicit ec: MyExecutionContext
) extends Tileable {

  def gdal2Tiles(imagePath: String): Future[String] = ???

}
