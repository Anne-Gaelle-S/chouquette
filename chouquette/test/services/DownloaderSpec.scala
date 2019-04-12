import org.scalatestplus.play._
import org.scalatestplus.play.guice._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.{ Path, Paths }

import javax.inject._
import play.core.server.Server
import play.api.routing.sird._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.http.ContentTypes
import play.api.libs.json._
import play.api.test._
import play.api.inject.guice.GuiceApplicationBuilder

import chouquette._
import chouquette.controllers.Auth
import chouquette.services.Downloader


class DownloaderSpec extends PlaySpec {

  implicit val ec: MyExecutionContext =
    new GuiceApplicationBuilder()
      .injector
      .instanceOf[MyExecutionContext]

  def deleteFile(path: Path): Boolean = deleteFile(path.toFile)
  def deleteFile(strPath: String): Boolean = deleteFile(new File(strPath))
  def deleteFile(file: File): Boolean = FileUtils.deleteQuietly(file)

  def fileContent(path: Path): String = fileContent(path.toFile)
  def fileContent(strPath: String): String = fileContent(new File(strPath))
  def fileContent(file: File): String = FileUtils.readFileToString(file, "utf8")

  def cleanup[T](runAfter: => Unit)(t: => T) = {
    try t
    finally runAfter
  }


  "Downloader.curl" should {

    var maybePathZip: Option[Path] = None

    "download image in tmp directory" in cleanup {
      maybePathZip.map(deleteFile)
      deleteFile("tmp/some")
    } {
      Server.withRouterFromComponents() { components =>
        import components.{ defaultActionBuilder => Action }
        {
          case GET(p"/img") => Action { Ok("file content") }
        }
      } { implicit port =>

        val future = new Downloader("tmp/some/folder")
          .curl(s"http://localhost:$port/img", Auth("titi", "toto"))

        val pathZip = Await.result(future, 10 seconds)
        maybePathZip = Some(pathZip)

        pathZip.toString must fullyMatch regex
          """tmp/some/folder/[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}\.zip"""
        fileContent(pathZip) mustBe "file content"

      }
    }

  }


  "Downloader.curl" should {

    val keep = new File("testutils/unzip.zip")
    val copy = new File("tmp/unzip-copy.zip")
    FileUtils.copyFile(keep, copy)
    var maybeExtracted: Option[Path] = None

    "unzip zip file and delete zip file" in cleanup {
      maybeExtracted.map(deleteFile)
    } {

      val future = new Downloader(ec).unzip(copy.toPath)

      new File("tmp/img") mustBe 'isDirectory

      val extracted = Await.result(future, 10 seconds)
      maybeExtracted = Some(extracted)
      Thread.sleep(500)

      copy must not be 'exists

      extracted.toFile mustBe 'isDirectory
      extracted.toString mustBe "tmp/unzip-copy"

      extracted.resolve("unzip").toFile mustBe 'exists
      extracted.resolve("unzip").toFile mustBe 'isDirectory
      extracted.resolve("unzip/toto.txt").toFile mustBe 'exists
      extracted.resolve("unzip/toto.txt").toFile mustBe 'isFile

    }

  }


  "Downloader.imageFromExtractedArchive" should {

    "take one image from extracted zip" in {

      val jp2Img = new Downloader(ec)
        .imageFromExtractedArchive(Paths.get("testutils/extracted-zip"))

      jp2Img.toString mustBe
        "testutils/extracted-zip/randomName/GRANULE/otherRandomName/IMG_DATA/toto.jp2"

    }

  }


  "Downloader.gdalwarp and Downloader.gdalinfo" should {

    FileUtils.forceMkdir(new File("tmp/toto"))

    "change coordinates to Lambert 93 and retrieve image metadata" in cleanup {
      deleteFile("tmp/toto")
    } {

      val downloader = new Downloader(ec)

      val future = downloader.gdalinfo(Paths.get("testutils/test.jp2"))
      val metaDataBefore = Await.result(future, 10 seconds)

      metaDataBefore mustBe
        MetaData(Point(499980, 5300040), Point(609780, 5190240))

      val future1 = downloader
        .gdalwarp(Paths.get("tmp/toto"), new File("testutils/test.jp2"))
      val tiffImage = Await.result(future1, 10 seconds)

      tiffImage.toString mustBe "tmp/toto/toto.tiff"

      tiffImage.toFile mustBe 'exists

      val future2 = downloader.gdalinfo(tiffImage)
      val metaDataAfter = Await.result(future2, 10 seconds)

      metaDataAfter mustBe
        MetaData(Point(699980.006,6750367.459),Point(809727.43,6640620.034))

    }

  }


  "Downloader.gdalwarp" should {

    "fail for invalid file" in {

      val future = new Downloader(ec)
        .gdalwarp(Paths.get("tmp/toto"), new File("testutils/unzip.zip"))
      val thrown = the [Exception] thrownBy Await.result(future, 10 seconds)

      thrown.getMessage must startWith ("Command failed: gdalwarp")

    }

  }


  "Downloader.cleanup" should {

    "remove extacted folder" in {

      val titi = new File("tmp/titi")
      FileUtils.forceMkdir(titi)

      titi mustBe 'isDirectory

      new Downloader(ec)
        .cleanup(DownloadResult(
          "tmp/titi",
          "unused",
          MetaData(Point(0,1),Point(2,3))))

      titi must not be 'exists

    }

  }

}
