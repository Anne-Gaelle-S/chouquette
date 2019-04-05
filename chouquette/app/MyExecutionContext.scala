package chouquette

import scala.concurrent.ExecutionContext

import javax.inject._
import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext
import com.google.inject.ImplementedBy


@ImplementedBy(classOf[MyExecutionContextImpl])
trait MyExecutionContext extends ExecutionContext {
  val system: ActorSystem
}

@Singleton
class MyExecutionContextImpl @Inject()(_system: ActorSystem)
  extends CustomExecutionContext(_system, "my.executor")
     with MyExecutionContext {
  val system = _system
}
