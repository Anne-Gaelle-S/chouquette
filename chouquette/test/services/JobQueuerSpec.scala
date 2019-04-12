import org.scalatestplus.play._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import chouquette._
import chouquette.services._


class JobQueuerSpec extends PlaySpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  "JobQueuer" should {

    "do stuff" in {

      new JobQueuer().queueMaxSize mustBe 10
      new JobQueuer(3).queueMaxSize mustBe 3
      an [IllegalArgumentException] must be thrownBy new JobQueuer(-4)

      new JobQueuer().runningJobs.size mustBe 0
      new JobQueuer().finishedJobs.size mustBe 0

      // submit
      val queuer = new JobQueuer(3)
      val future = Future {
        Thread.sleep(1000)
        (MetaData(Point(0,1),Point(2,3)), "finished")
      }
      val uuid = queuer.submit(future)

      uuid must fullyMatch regex
        """[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}"""
      queuer.runningJobs.size mustBe 1
      queuer.finishedJobs mustBe Map.empty

      queuer.canBeSubmitted mustBe true

      Await.result(future, 2 seconds)
      Thread.sleep(500)

      queuer.runningJobs mustBe Map.empty
      queuer.finishedJobs mustBe
        Map(uuid -> Success((MetaData(Point(0,1),Point(2,3)), "finished")))

      // removeFinishedJob
      queuer.removeFinishedJob(uuid)

      queuer.runningJobs mustBe Map.empty
      queuer.finishedJobs mustBe Map.empty

      val futures = Range(0, 3)
        .map { _ =>
          val future = Future {
            Thread.sleep(1000)
            (MetaData(Point(0,1),Point(2,3)), "result")
          }
          queuer.submit(future)
          future
        }
        .toSeq

      queuer.canBeSubmitted mustBe false

      Await.result(Future.sequence(futures), 2 seconds)

      queuer.canBeSubmitted mustBe true

      queuer.runningJobs mustBe Map.empty
      queuer.finishedJobs.size mustBe 3

      // status
      queuer.status("toto") mustBe UnknownJob

      val future2 = Future {
        Thread.sleep(1000)
        (MetaData(Point(0,1),Point(2,3)), "success")
      }
      val uuid2 = queuer.submit(future2)

      queuer.status(uuid2) mustBe Running

      Await.result(future2, 2 seconds)
      Thread.sleep(500)

      queuer.status(uuid2) mustBe
        Finished((MetaData(Point(0,1),Point(2,3)), "success"))

      val future3 = Future {
        Thread.sleep(1000)
        throw new Exception("failed")
      }
      val uuid3 = queuer.submit(future3)

      queuer.status(uuid3) mustBe Running

      try {
        Await.result(future3, 2 seconds)
      } catch {
        case e: Exception => Unit
      }

      queuer.status(uuid3) mustBe Failed("failed")

    }

  }

}
