import org.scalatestplus.play._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.{ Path, Paths }

import play.api.inject.guice.GuiceApplicationBuilder

import chouquette._
import chouquette.services._


class TilerSpec extends PlaySpec {

  implicit val ec: MyExecutionContext =
    new GuiceApplicationBuilder()
      .injector
      .instanceOf[MyExecutionContext]

  def deleteFile(path: Path): Boolean = deleteFile(path.toFile)
  def deleteFile(strPath: String): Boolean = deleteFile(new File(strPath))
  def deleteFile(file: File): Boolean = FileUtils.deleteQuietly(file)

  def cleanup[T](runAfter: => Unit)(t: => T) = {
    try t
    finally runAfter
  }


  "Tiler.gdal2Tiles" should {

    FileUtils.forceMkdir(new File("tmp/warped"))

    "tile an image (and Tiler.cleanup should cleanup)" in cleanup {
      deleteFile("tmp/output")
      deleteFile("tmp/warped")
    } {

      val future1 = new Downloader(ec)
        .gdalwarp(Paths.get("tmp/warped"), new File("testutils/test.jp2"))
      val tiffImage = Await.result(future1, 10 seconds)

      tiffImage.toString mustBe "tmp/warped/warped.tiff"
      tiffImage.toFile mustBe 'isFile

      val future2 = new Tiler()
        .gdal2Tiles(DownloadResult(
          "tmp/output",
          "tmp/warped/warped.tiff",
          MetaData(Point(0,1),Point(2,3))))
      val tileResult = Await.result(future2, 10 seconds)

      tileResult.tiffPath mustBe "tmp/warped/warped.tiff"
      tileResult.tiles.base mustBe "tmp/output/tiles"
      tileResult.tiles.zDirectories.size mustBe 4
      tileResult.tiles.zDirectories(0).name mustBe "10"
      tileResult.tiles.zDirectories(1) mustBe ZDirectory("7",
        XDirectory("65",
          "82.png",
          "83.png"))
      tileResult.tiles.zDirectories(2).name mustBe "8"
      tileResult.tiles.zDirectories(3).name mustBe "9"

      // cleanup
      new Tiler()
        .cleanup(tileResult)

      new File("tmp/output/tiles") must not be 'exists
      new File("tmp/warped/warped.tiff") mustBe 'exists

    }


    "fail for invalid input" in {

      val future = new Tiler()
        .gdal2Tiles(DownloadResult(
          "tmp/whatever",
          "tmp/kaboum",
          MetaData(Point(0,1),Point(2,3))))
      val thrown = the [Exception] thrownBy Await.result(future, 10 seconds)

      thrown.getMessage must startWith ("Command failed: gdal2tiles.py")

    }

  }

}
