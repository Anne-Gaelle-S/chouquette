package chouquette.services

import scala.concurrent._
import scala.util.Try

import java.util.UUID.randomUUID

import javax.inject._

import chouquette._
import chouquette.controllers.JobQueueable


@Singleton
class JobQueuer(
  _queueMaxSize: Int=10
)(
  implicit ec: ExecutionContext
) extends JobQueueable {

  @Inject def this(ec: ExecutionContext) = this()(ec)

  require(_queueMaxSize > 0, "Queue max size must be positive")

  val queueMaxSize = _queueMaxSize


  var runningJobs: Map[String, Future[(MetaData, String)]] = Map.empty
  var finishedJobs: Map[String, Try[(MetaData, String)]] = Map.empty


  def canBeSubmitted: Boolean = runningJobs.size < queueMaxSize

  def submit(job: Future[(MetaData, String)]): String = {
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

  def addRunningJob(uuid: String, job: Future[(MetaData, String)]): Unit =
    runningJobs += ((uuid, job))

  def removeRunningJob(uuid: String): Unit =
    runningJobs -= uuid

  def addFinishedJob(uuid: String, tryResult: Try[(MetaData, String)]): Unit =
    finishedJobs += ((uuid, tryResult))

  def removeFinishedJob(uuid: String): Unit =
    finishedJobs -= uuid

}
