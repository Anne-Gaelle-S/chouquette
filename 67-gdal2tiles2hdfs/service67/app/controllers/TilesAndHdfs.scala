package service67.controllers

import scala.concurrent._

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.ImplementedBy

import service67.MyExecutionContext
import service67.services._


@ImplementedBy(classOf[Downloader])
trait Downloadable {
  def downloadImage(url: String)
                   (implicit ec: ExecutionContext): Future[String]
}

@ImplementedBy(classOf[Tiler])
trait Tileable {
  def gdal2Tiles(imagePath: String)
                (implicit ec: ExecutionContext): Future[String]
}

@ImplementedBy(classOf[HdfsPutter])
trait HdfsPuttable {
  def putHdfs(hdfsPath: String)
             (tilesPath: String)
             (implicit ec: ExecutionContext): Future[String]
}


@Singleton
class TilesAndHdfs @Inject()(
  downloader: Downloadable,
  tiler: Tileable,
  hdfsPutter: HdfsPuttable,
  cc: ControllerComponents
)(
  implicit ec: MyExecutionContext
) extends AbstractController(cc) {

  def downloadThenTileThenHdfs(url: String, hdfsPath: String): String = {
    downloader.downloadImage(url)
      .flatMap(tiler.gdal2Tiles)
      .flatMap(hdfsPutter.putHdfs(hdfsPath))
    hdfsPath
  }

  def gdal2tiles2hdfs = Action(parse.json) { request =>
    (for {
      url <- (request.body \ "url").validate[String]
      hdfsPath <- (request.body \ "hdfsPath").validate[String]
    } yield downloadThenTileThenHdfs(url, hdfsPath))
      .map(statusRoute => Ok(Json.obj("status" -> JsString(statusRoute))))
      .getOrElse(BadRequest("""Invalid json: required fields :
        |- "url": string
        |- "hdfsPath": string""".stripMargin))
  }

}
