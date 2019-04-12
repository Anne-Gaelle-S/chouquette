package chouquette.services

import scala.concurrent._
import sys.process._

import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.{ Paths, Path }

import javax.inject._

import chouquette._
import chouquette.controllers.Tileable


@Singleton
class Tiler @Inject()()(
  implicit ec: MyExecutionContext
) extends Tileable {

  // cleanup tiles base directory
  def cleanup(tileResult: TileResult): Unit =
    FileUtils.deleteQuietly(new File(tileResult.tiles.base))


  def gdal2Tiles(downloadResult: DownloadResult): Future[TileResult] = {
    val output = Paths.get(downloadResult.extractedDir).resolve("tiles")
    val cmd = s"gdal2tiles.py ${downloadResult.tiffPath} $output"
    println(s"Running command: $cmd")
    Future(cmd !)
      .map(exitCode =>
        if (exitCode != 0) throw new Exception(s"Command failed: $cmd"))
      .map(_ => TileResult(
        downloadResult.tiffPath,
        TilesDirectory.fromOutput(output)))
  }

}
