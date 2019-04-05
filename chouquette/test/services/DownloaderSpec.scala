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

    "download image" in {

      Server.withRouterFromComponents() { components =>
        import components.{ defaultActionBuilder => Action }
        {
          case GET(p"/") => Action { Ok("file content") }
        }
      } { implicit port =>
        WsTestClient.withClient { client =>
          val tryFuture =
            new Downloader(client).downloadImage(s"http://localhost:$port/")

          tryFuture mustBe 'isSuccess
          val tempFile = Await.result(tryFuture.get, 10 seconds)
          tempFile must fullyMatch regex
            """tmp/img/[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}\.tiff"""
          fileContent(tempFile) mustBe Some("file content")
        }
      }

      cleanUpFile("tmp/img")

    }

  }

}
