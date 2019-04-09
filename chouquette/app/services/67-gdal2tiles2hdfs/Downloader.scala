package chouquette.services

import scala.concurrent._
import scala.util.Try

import java.io.{ File, OutputStream }
import java.nio.file.{ Paths, Path, Files }
import java.util.UUID.randomUUID

import akka.Done
import akka.util.ByteString
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import javax.inject._
import play.api.libs.ws._

import chouquette.MyExecutionContext
import chouquette.controllers.Downloadable


@Singleton
class Downloader(
  ws: WSClient,
  tmpFolder: String
)(
  implicit ec: MyExecutionContext
) extends Downloadable {

  @Inject def this(
    ws: WSClient,
    ec: MyExecutionContext
  ) = this(ws, "tmp/img")(ec)

  implicit val materializer = ActorMaterializer()(ec.system)

  // Future returns the path where the image was saved on local storage.
  def downloadImage(imageUrl: String): Future[String] = {
    val uuid = randomUUID().toString
    val path = Paths.get(tmpFolder).resolve(s"$uuid.tiff")
    createFileIfNeeded(path)
    downloadToPath(imageUrl, path).map(_ => path.toString)
  }

  def createFileIfNeeded(path: Path): Unit = {
    val file = path.toFile
    createParentIfNeeded(path)
    Files.createFile(path)
  }

  def createParentIfNeeded(path: Path): Unit = {
    val parent = path.getParent
    if (parent != null) Files.createDirectories(parent)
  }

  def downloadToPath(url: String, path: Path): Future[Done] =
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
