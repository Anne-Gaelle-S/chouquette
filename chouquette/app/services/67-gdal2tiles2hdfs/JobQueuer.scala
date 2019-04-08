package chouquette.services

import scala.concurrent._

import java.util.UUID.randomUUID

import javax.inject._

import chouquette.controllers.JobQueueable


@Singleton
class JobQueuer(_queueMaxSize: Int=10) extends JobQueueable {

  require(_queueMaxSize > 0, "Queue max size must be positive")

  val queueMaxSize = _queueMaxSize


  var runningJobs = Map.empty[String, Future[String]]
  var finishedJobs = Map.empty[String, String]


  def canBeSubmitted: Boolean = ???

  def submit(job: Future[String]): String = {
    val uuid = randomUUID().toString
    runningJobs = runningJobs + ((uuid, job))
    uuid
  }

}
