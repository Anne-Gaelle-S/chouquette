package chouquette.controllers

import scala.concurrent._
import scala.util.{ Try, Success, Failure }

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import com.google.inject.ImplementedBy

import chouquette._
import chouquette.controllers.routes.{ TilesAndHdfs => ReverseTilesAndHdfs }
import chouquette.services._


case class Auth(username: String, password: String)


@ImplementedBy(classOf[JobQueuer])
trait JobQueueable {
  def canBeSubmitted: Boolean
  def submit(job: Future[JobResult]): String
  def status(uuid: String): JobStatus
  def removeFinishedJob(uuid: String): Unit
}

@ImplementedBy(classOf[Downloader])
// Downloads archive, extracts it, keep one image of it, warps it and gets
// metadata.
trait Downloadable {
  def downloadImage(imageUrl: String, auth: Auth): Future[DownloadResult]
  def cleanup(downloadResult: DownloadResult): Unit
}

@ImplementedBy(classOf[Tiler])
trait Tileable {
  def gdal2Tiles(downloadResult: DownloadResult): Future[TileResult]
  def cleanup(tileResult: TileResult): Unit
}


case class SSHServer(host: String, user: String, password: String)

@ImplementedBy(classOf[HDFSPutter])
trait HDFSPuttable {
  def putHDFS(sshServer: SSHServer, hdfsPath: String)
             (tileResult: TileResult): Future[String]
}


@Singleton
class TilesAndHdfs @Inject()(
  jobQueuer: JobQueueable,
  downloader: Downloadable,
  tiler: Tileable,
  hdfsPutter: HDFSPuttable,
  cc: ControllerComponents
)(
  implicit ec: ExecutionContext,
  myEc: MyExecutionContext
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
      val sshServer = SSHServer(hdfsHost, hdfsUser, hdfsPass)
      trySubmitJob(imageUrl, auth, sshServer, hdfsPass)
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
      sshServer: SSHServer,
      hdfsPath: String
  ): Try[String] =
    if (jobQueuer.canBeSubmitted) {
      val job = downloader
        .downloadImage(imageUrl, auth)
        .flatMap(tileThenHdfs(sshServer, hdfsPath))
      val jobId = jobQueuer.submit(job)
      Success(ReverseTilesAndHdfs.status(jobId).path)
    } else Failure(new Exception("Couldn't submit job"))

  def tileThenHdfs(
      sshServer: SSHServer,
      hdfsPath: String
  )(
      downloadResult: DownloadResult
  ): Future[JobResult] =
    tiler.gdal2Tiles(downloadResult)
      .flatMap(hdfs(sshServer, hdfsPath)(downloadResult))

  def hdfs(
      sshServer: SSHServer,
      hdfsPath: String
  )(
      downloadResult: DownloadResult
  )(
      tileResult: TileResult
  ): Future[JobResult] = {
    val res = hdfsPutter.putHDFS(sshServer, hdfsPath)(tileResult)
      .map(JobResult(downloadResult.metaData, _))
    res.onComplete { _ =>
      downloader.cleanup(downloadResult)
      tiler.cleanup(tileResult)
    }
    res
  }


  // GET /status/<jobId>
  def status(jobId: String): Action[AnyContent] = Action {
    jobQueuer.status(jobId) match {
      case UnknownJob => NotFound(s"Unknown job: $jobId")
      case Running => Accepted("Job not finished yet")
      case complete: JobComplete => caseComplete(jobId, complete)
    }
  }

  def caseComplete(jobId: String, complete: JobComplete): Result = {
    jobQueuer.removeFinishedJob(jobId)
    complete match {
      case Failed(message) =>
        InternalServerError(s"Job exited with error: $message")
      case Finished(result) => Ok(Json.toJson(result))
    }
  }

}
