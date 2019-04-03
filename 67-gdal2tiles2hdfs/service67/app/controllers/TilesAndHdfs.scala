package service67.controllers

import scala.concurrent._
import scala.util.{ Try, Success, Failure }

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.ImplementedBy

import service67.services._


@ImplementedBy(classOf[Downloader])
trait Downloadable {
  def downloadImage(url: String): Try[Future[String]]
}

@ImplementedBy(classOf[Tiler])
trait Tileable {
  def gdal2Tiles(imagePath: String): Future[String]
}

@ImplementedBy(classOf[HdfsPutter])
trait HdfsPuttable {
  def putHdfs(hdfsPath: String)
             (tilesPath: String): Future[String]
}


@Singleton
class TilesAndHdfs @Inject()(
  downloader: Downloadable,
  tiler: Tileable,
  hdfsPutter: HdfsPuttable,
  cc: ControllerComponents
)(
  implicit ec: ExecutionContext
) extends AbstractController(cc) {

  def gdal2tiles2hdfs = Action(parse.json) { request =>
    (for {
      url <- (request.body \ "url").validate[String]
      hdfsPath <- (request.body \ "hdfsPath").validate[String]
    } yield downloadThenTileThenHdfs(url, hdfsPath))
      .map {
        case Success(res) => Ok(Json.obj("status" -> JsString(res)))
        case Failure(e) => InternalServerError(e.getMessage)
      }
      .getOrElse(BadRequest("""Invalid json: required fields :
        |- "url": string
        |- "hdfsPath": string""".stripMargin))
  }

  def downloadThenTileThenHdfs(url: String, hdfsPath: String): Try[String] =
    downloader.downloadImage(url)
      .map(_.flatMap(tiler.gdal2Tiles))
      .map(_.flatMap(hdfsPutter.putHdfs(hdfsPath)))
      .map(_ => "adedigado")
      // .map(_ => hdfsPath)

}
