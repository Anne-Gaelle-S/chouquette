package service67.controllers

import scala.concurrent._
import scala.util.{ Try, Success, Failure }

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.ImplementedBy

import service67.controllers.routes.{ TilesAndHdfs => ReverseTilesAndHdfs }
import service67.services._


case class HdfsServer(host: String, user: String, password: String)


@ImplementedBy(classOf[JobQueuer])
trait JobQueueable {
  def canBeSubmitted: Boolean
  def submit(job: Future[String]): String
}

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
  def putHdfs(hdfsServer: HdfsServer, hdfsPath: String)
             (tilesPath: String): Future[String]
}


@Singleton
class TilesAndHdfs @Inject()(
  jobQueuer: JobQueueable,
  downloader: Downloadable,
  tiler: Tileable,
  hdfsPutter: HdfsPuttable,
  cc: ControllerComponents
)(
  implicit ec: ExecutionContext
) extends AbstractController(cc) {

  // POST /gdal2tiles2hdfs
  def gdal2tiles2hdfs = Action(parse.json) { request =>
    (for {
      imageUrl <- (request.body \ "imageUrl").validate[String]
      hdfsHost <- (request.body \ "hdfsHost").validate[String]
      hdfsUser <- (request.body \ "hdfsUser").validate[String]
      hdfsPass <- (request.body \ "hdfsPass").validate[String]
      hdfsPath <- (request.body \ "hdfsPath").validate[String]
    } yield {
      val hdfsServer = HdfsServer(hdfsHost, hdfsUser, hdfsPass)
      trySubmitJob(imageUrl, hdfsServer, hdfsPass)
    })
      .map {
        case Success(res) => Created(Json.obj("status" -> JsString(res)))
        case Failure(e) => InternalServerError(e.getMessage)
      }
      .getOrElse(BadRequest("""Invalid json: required fields :
        |"imageUrl": string
        |"hdfsHost": string
        |"hdfsUser": string
        |"hdfsPass": string
        |"hdfsPath": string""".stripMargin))
  }

  def trySubmitJob(
      imageUrl: String,
      hdfsServer: HdfsServer,
      hdfsPath: String
  ): Try[String] =
    if (jobQueuer.canBeSubmitted)
      downloadThenTileThenHdfs(imageUrl, hdfsServer, hdfsPath)
        .map(job => {
          val jobId = jobQueuer.submit(job)
          ReverseTilesAndHdfs.status(jobId).path
        })
    else Failure(new Exception("Couldn't submit job"))

  def downloadThenTileThenHdfs(
      imageUrl: String,
      hdfsServer: HdfsServer,
      hdfsPath: String
  ): Try[Future[String]] =
      downloader.downloadImage(imageUrl)
        .map(_.flatMap(tileThenHdfs(hdfsServer, hdfsPath)))

  def tileThenHdfs(hdfsServer: HdfsServer, hdfsPath: String)
                  (imgPath: String): Future[String] =
    for {
      tilesPath <- tiler.gdal2Tiles(imgPath)
      hdfsPathBis <- hdfsPutter.putHdfs(hdfsServer, hdfsPath)(tilesPath)
    } yield hdfsPathBis


  // GET /status/<jobId>
  def status(jobId: String): Action[AnyContent] = ???
}
