package chouquette.services

import scala.concurrent._

import javax.inject._

import chouquette.MyExecutionContext
import chouquette.controllers.Tileable


@Singleton
class Tiler @Inject()()(
  implicit ec: MyExecutionContext
) extends Tileable {

  def gdal2Tiles(imagePath: String): Future[String] = ???

}
