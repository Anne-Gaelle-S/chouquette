package chouquette.services

import scala.concurrent._
import scala.util.{ Try, Success, Failure }

import java.util.UUID.randomUUID

import javax.inject._

import chouquette.controllers.JobQueueable


sealed trait JobStatus

object JobStatus {
  val fromTry: Try[String] => JobStatus = {
    case Success(result) => Finished(result)
    case Failure(exception) => Failed(exception.getMessage)
  }
}


case object UnknownJob extends JobStatus

case object Running extends JobStatus

case class Finished(result: String) extends JobStatus

case class Failed(message: String) extends JobStatus


@Singleton
class JobQueuer(
  _queueMaxSize: Int=10
)(
  implicit ec: ExecutionContext
) extends JobQueueable {

  @Inject def this(ec: ExecutionContext) = this()(ec)

  require(_queueMaxSize > 0, "Queue max size must be positive")

  val queueMaxSize = _queueMaxSize


  var runningJobs: Map[String, Future[String]] = Map.empty
  var finishedJobs: Map[String, Try[String]] = Map.empty


  def canBeSubmitted: Boolean = runningJobs.size < queueMaxSize

  def submit(job: Future[String]): String = {
    val uuid = randomUUID().toString
    addRunningJob(uuid, job)
    job.onComplete { tryResult =>
      removeRunningJob(uuid)
      addFinishedJob(uuid, tryResult)
    }
    uuid
  }

  def status(uuid: String): JobStatus =
    if (runningJobs.contains(uuid)) Running
    else
      finishedJobs.get(uuid)
        .map(JobStatus.fromTry)
        .getOrElse(UnknownJob)

  def addRunningJob(uuid: String, job: Future[String]): Unit =
    runningJobs += ((uuid, job))

  def removeRunningJob(uuid: String): Unit =
    runningJobs -= uuid

  def addFinishedJob(uuid: String, tryResult: Try[String]): Unit =
    finishedJobs += ((uuid, tryResult))

  def removeFinishedJob(uuid: String): Unit =
    finishedJobs -= uuid

}
