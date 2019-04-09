import org.scalatestplus.play._
import org.scalatestplus.play.guice._

import scala.io.Source
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import java.io.File

import javax.inject._
import play.core.server.Server
import play.api.routing.sird._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.http.ContentTypes
import play.api.libs.json._
import play.api.test._
import play.api.inject.guice.GuiceApplicationBuilder

import chouquette.MyExecutionContext
import chouquette.services.Downloader


class DownloaderSpec extends PlaySpec {

  implicit val ec: MyExecutionContext =
    new GuiceApplicationBuilder()
      .injector
      .instanceOf[MyExecutionContext]

  def fileContent(file: String): Option[String] = {
    var source = Source.fromFile(file)
    try {
      Some(source.mkString)
    } catch {
      case e: Throwable => None
    } finally source.close()
  }

  def cleanUpFile(file: String): Unit =
    try {
      deleteRecursively(new File(file))
    } catch {
      case e: Throwable => println(s"Exception while deleting file: $e")
    }

  def deleteRecursively(file: File): Unit = {
    if (file.isDirectory)
      file.listFiles.foreach(deleteRecursively)
    if (file.exists && !file.delete)
      throw new Exception(s"Unable to delete ${file.getAbsolutePath}")
  }


  "Downloader" should {

    "download image in tmp directory" in {

      val tmpDir = "tmp/img"

      Server.withRouterFromComponents() { components =>
        import components.{ defaultActionBuilder => Action }
        {
          case GET(p"/img") => Action { Ok("file content") }
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val future = new Downloader(client, tmpDir)
            .downloadImage(s"http://localhost:$port/img")

          val tempFile = Await.result(future, 10 seconds)

          tempFile must fullyMatch regex
            tmpDir+"""/[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}\.tiff"""
          fileContent(tempFile) mustBe Some("file content")
        }
      }

      cleanUpFile(tmpDir)

    }


    "download image in current dir" in {

      val tmpDir = ""

      Server.withRouterFromComponents() { components =>
        import components.{ defaultActionBuilder => Action }
        {
          case GET(p"/img2") => Action { Ok("toto") }
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val future = new Downloader(client, tmpDir)
            .downloadImage(s"http://localhost:$port/img2")

          val tempFile = Await.result(future, 10 seconds)

          fileContent(tempFile) mustBe Some("toto")

          cleanUpFile(tempFile)
        }
      }

    }

  }

}
