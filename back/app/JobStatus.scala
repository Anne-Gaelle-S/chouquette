package chouquette

import scala.util.{ Try, Success, Failure }


sealed trait JobStatus

object JobStatus {
  val fromTry: Try[JobResult] => JobStatus = {
    case Success(result) => Finished(result)
    case Failure(exception) => Failed(exception.getMessage)
  }
}

case object UnknownJob extends JobStatus

case object Running extends JobStatus

sealed trait JobComplete extends JobStatus


case class Finished(result: JobResult) extends JobComplete

case class Failed(message: String) extends JobComplete
