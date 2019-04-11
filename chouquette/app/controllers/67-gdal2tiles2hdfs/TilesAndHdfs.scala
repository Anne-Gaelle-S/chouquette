package chouquette.controllers

import scala.concurrent._
import scala.util.{ Try, Success, Failure }

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.ImplementedBy

import chouquette.{ JobStatus, DownloadResult, MetaData }
import chouquette.controllers.routes.{ TilesAndHdfs => ReverseTilesAndHdfs }
import chouquette.services._


case class HdfsServer(host: String, user: String, password: String)


case class Auth(username: String, password: String)


@ImplementedBy(classOf[JobQueuer])
trait JobQueueable {
  def canBeSubmitted: Boolean
  def submit(job: Future[(MetaData, String)]): String
  def status(uuid: String): JobStatus
}

@ImplementedBy(classOf[Downloader])
trait Downloadable {
  def downloadImage(imageUrl: String, auth: Auth): Future[DownloadResult]
  def cleanup(downloadResult: DownloadResult): Unit
}

@ImplementedBy(classOf[Tiler])
trait Tileable {
  def gdal2Tiles(downloadResult: DownloadResult): Future[String]
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
      pepsUser <- (request.body \ "pepsUser").validate[String]
      pepsPass <- (request.body \ "pepsPass").validate[String]
      hdfsHost <- (request.body \ "hdfsHost").validate[String]
      hdfsUser <- (request.body \ "hdfsUser").validate[String]
      hdfsPass <- (request.body \ "hdfsPass").validate[String]
      hdfsPath <- (request.body \ "hdfsPath").validate[String]
    } yield {
      val auth = Auth(pepsUser, pepsPass)
      val hdfsServer = HdfsServer(hdfsHost, hdfsUser, hdfsPass)
      trySubmitJob(imageUrl, auth, hdfsServer, hdfsPass)
    })
      .map {
        case Success(res) => Created(Json.obj("status" -> JsString(res)))
        case Failure(e) => InternalServerError(e.getMessage)
      }
      .getOrElse(BadRequest("""Invalid json: required fields :
        |"imageUrl": string
        |"pepsUser": string
        |"pepsPass": string
        |"hdfsHost": string
        |"hdfsUser": string
        |"hdfsPass": string
        |"hdfsPath": string""".stripMargin))
  }

  def trySubmitJob(
      imageUrl: String,
      auth: Auth,
      hdfsServer: HdfsServer,
      hdfsPath: String
  ): Try[String] =
    if (jobQueuer.canBeSubmitted) {
      val job = downloader
        .downloadImage(imageUrl, auth)
        .flatMap(tileThenHdfs(hdfsServer, hdfsPath))
      val jobId = jobQueuer.submit(job)
      Success(ReverseTilesAndHdfs.status(jobId).path)
    } else Failure(new Exception("Couldn't submit job"))

  def tileThenHdfs(
      hdfsServer: HdfsServer,
      hdfsPath: String
  )(
      downloadResult: DownloadResult
  ): Future[(MetaData, String)] = {
    val res = for {
        tilesPath <- tiler.gdal2Tiles(downloadResult)
        hdfsPathBis <- hdfsPutter.putHdfs(hdfsServer, hdfsPath)(tilesPath)
      } yield (downloadResult.metaData, hdfsPathBis)
    res.onComplete(_ => downloader.cleanup(downloadResult))
    res
  }



  // GET /status/<jobId>
  def status(jobId: String): Action[AnyContent] = ???
}
