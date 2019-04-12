package chouquette

import scala.util.{ Try, Success, Failure }


sealed trait JobStatus

object JobStatus {
  val fromTry: Try[(MetaData, String)] => JobStatus = {
    case Success(result) => Finished(result)
    case Failure(exception) => Failed(exception.getMessage)
  }
}

case object UnknownJob extends JobStatus

case object Running extends JobStatus

case class Finished(result: (MetaData, String)) extends JobStatus

case class Failed(message: String) extends JobStatus
