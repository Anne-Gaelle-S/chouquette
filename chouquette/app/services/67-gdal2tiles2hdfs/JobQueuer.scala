package chouquette.services

import scala.concurrent._

import javax.inject._

import chouquette.controllers.JobQueueable


@Singleton
class JobQueuer @Inject()()() extends JobQueueable {

  val queueMaxSize = 10

  var jobs = Map.empty[String, Future[String]]


  def canBeSubmitted: Boolean = ???

  def submit(job: Future[String]): String = ???

}
