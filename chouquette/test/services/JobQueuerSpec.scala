import org.scalatestplus.play._

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._

import chouquette.services.JobQueuer


class JobQueuerSpec extends PlaySpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  "JobQueuer" should {

    "construct" in {

      new JobQueuer().queueMaxSize mustBe 10
      new JobQueuer(3).queueMaxSize mustBe 3
      an [IllegalArgumentException] must be thrownBy new JobQueuer(-4)

      new JobQueuer().runningJobs.size mustBe 0
      new JobQueuer().finishedJobs.size mustBe 0

    }

  }


  "JobQueuer.submit" should {

    "add job to queue" in {

      val queuer = new JobQueuer()
      val future = Future("finished")
      val uuid = queuer.submit(future)

      uuid must fullyMatch regex
        """[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}"""
      queuer.runningJobs.size mustBe 1

    }

    "remove it when job is finished" in {

      val queuer = new JobQueuer()
      val future = Future("finished")
      val uuid = queuer.submit(future)
      Await.result(future, 1 seconds)

    }

  }

}
