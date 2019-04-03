package service67.services

import scala.concurrent._
import scala.util.{ Try, Success, Failure }

import java.io.{ File, OutputStream }
import java.nio.file.{ Paths, Path, Files }
import java.util.UUID.randomUUID

import akka.Done
import akka.util.ByteString
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import javax.inject._
import play.api.libs.ws._

import service67.MyExecutionContext
import service67.controllers.Downloadable


@Singleton
class Downloader @Inject()(
  ws: WSClient
)(
  implicit ec: MyExecutionContext
) extends Downloadable {

  implicit val materializer = ActorMaterializer()(ec.system)

  // Future returns the path where the image was saved on local storage.
  def downloadImage(url: String): Try[Future[String]] = {
    val uuid = randomUUID().toString
    val strPath = s"tmp/img/$uuid.tiff"
    createFileIfNeeded(strPath)
      .map(downloadToPath(url))
      .map(_.map(_ => strPath))
  }

  def createFileIfNeeded(strPath: String): Try[Path] = {
    val path = Paths.get(strPath)
    val file = path.toFile
    if (file.exists) checkIfDirectory(file) else createFile(path)
  }

  def checkIfDirectory(file: File): Try[Path] =
    if (file.isFile) Failure(
      new Exception("Can't download image: temp file already exists"))
    else Failure(
      new Exception("Can't download image: temp file is a directory"))

  def createFile(path: Path): Try[Path] = {
    createParentIfNeeded(path)
    Success(Files.createFile(path))
  }

  def createParentIfNeeded(path: Path): Unit = {
    val parent = path.getParent
    if (parent != null) Files.createDirectories(parent)
  }

  def downloadToPath(url: String)(path: Path): Future[Done] =
    ws.url(url)
      .withMethod("GET")
      .stream()
      .flatMap(handleResponse(path))

  def handleResponse(path: Path)(response: WSResponse): Future[Done] = {
    val outputStream = Files.newOutputStream(path)
    // materialize and run the stream
    response.bodyAsSource
      .runWith(sink(outputStream))
      .andThen(cleanupStream(outputStream))
  }

  // The sink that writes to the output stream
  def sink(outputStream: OutputStream): Sink[ByteString, Future[Done]] =
    Sink.foreach[ByteString](bytes => outputStream.write(bytes.toArray))

  def cleanupStream(
      outputStream: OutputStream
  ): PartialFunction[Try[Done], Done] = {
    case result =>
      // Close the output stream whether there was an error or not
      outputStream.close()
      // Get the result or rethrow the error
      result.get
  }

}
