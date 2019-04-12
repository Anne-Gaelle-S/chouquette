package chouquette.services

import scala.concurrent._
import scala.util.Try
import sys.process._

import org.apache.commons.io.FileUtils
import java.io.{ IOException, File }
import java.nio.file.{ Paths, Path }
import java.util.UUID.randomUUID

import javax.inject._
import play.api.libs.json._

import chouquette.{ MyExecutionContext, DownloadResult, MetaData }
import chouquette.controllers.{ Downloadable, Auth }


@Singleton
class Downloader(
  tmpFolderStr: String
)(
  implicit ec: MyExecutionContext
) extends Downloadable {

  @Inject def this(ec: MyExecutionContext) = this("tmp/img")(ec)

  s"mkdir -p $tmpFolderStr" !
  val tmpFolder = Paths.get(tmpFolderStr)


  // cleanup extracted base directory (and tiff image as it is inside it)
  def cleanup(downloadResult: DownloadResult): Unit =
    FileUtils.deleteQuietly(new File(downloadResult.extractedDir))


  // Future returns the path where the image was saved on local storage.
  def downloadImage(
      imageUrl: String,
      auth: Auth
  ): Future[DownloadResult] =
    (for {
      path <- curl(imageUrl, auth)
      extractedDir <- unzip(path)
    } yield (extractedDir, imageFromExtractedArchive(extractedDir)))
      .flatMap(tiffAndMetaData)

  val tiffAndMetaData: ((Path, File)) => Future[DownloadResult] = {
    case (extractedDir, image) => for {
      tiffImage <- gdalwarp(extractedDir, image)
      metaData <- gdalinfo(tiffImage)
    } yield DownloadResult(extractedDir, tiffImage, metaData)
  }

  def runCommand[T](cmd: String, f: => T): Future[T] = Future {
    println(s"Running command: $cmd")
    try {
      f
    } catch {
      case e: IOException if e.getMessage startsWith "Cannot run program" =>
        val program = cmd.split(" ").head
        throw new Exception(s"Program doesn't exist: $program")
    }
  }

  def throwIfNotExit0(cmd: String): Future[Unit] =
    runCommand(cmd, cmd !)
      .map(exitCode =>
        if (exitCode != 0) throw new Exception(s"Command failed: $cmd"))

  def curl(url: String, auth: Auth): Future[Path] = {
    val uuid = randomUUID().toString
    val zipPath = tmpFolder.resolve(s"$uuid.zip")
    val cmd =
      s"curl -k --basic -u ${auth.username}:${auth.password} $url -o $zipPath"
    throwIfNotExit0(cmd)
      .map(_ => zipPath)
  }

  def unzip(path: Path): Future[Path] = {
    val extractedDir = path.toString
      .split("""\.""")
      .init
      .mkString(".")
    val cmd = s"unzip $path -d $extractedDir"
    val res = throwIfNotExit0(cmd)
      .map(_ => Paths.get(extractedDir))
    res.onComplete(_ => FileUtils.deleteQuietly(path.toFile))
    res
  }

  def imageFromExtractedArchive(extractedDir: Path): File = {
    // $extracted/[randomName]/GRANULE/[otherRandomName]/IMG_DATA/*.jp2
    val randomName = extractedDir.toFile.listFiles.head.toPath
    val granule = randomName.resolve("GRANULE").toFile
    val otherRandomName = granule.listFiles.head.toPath
    val imgData = otherRandomName.resolve("IMG_DATA").toFile
    imgData.listFiles.sortBy(_.toString).last // Why last? Why not?
  }

  def gdalinfo(image: Path): Future[MetaData] = {
    val cmd = s"gdalinfo -json $image"
    runCommand(cmd, cmd !!)
      .map(output =>
        MetaData.fromJson(Json.parse(output))
          .getOrElse(throw new Exception("Couldn't parse metadata")))
  }

  def gdalwarp(extractedDir: Path, image: File): Future[Path] = {
    val tiffImage = extractedDir.resolve(s"${extractedDir.getFileName}.tiff")
    val cmd = s"gdalwarp -t_srs EPSG:2154 $image $tiffImage"
    throwIfNotExit0(cmd)
      .map(_ => tiffImage)
  }

}
