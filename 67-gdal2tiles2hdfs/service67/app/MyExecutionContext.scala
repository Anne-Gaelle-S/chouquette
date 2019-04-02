package service67

import scala.concurrent.ExecutionContext

import javax.inject._
import akka.actor.ActorSystem
import play.api.libs.concurrent.CustomExecutionContext
import com.google.inject.ImplementedBy


@ImplementedBy(classOf[MyExecutionContextImpl])
trait MyExecutionContext extends ExecutionContext

@Singleton
class MyExecutionContextImpl @Inject()(system: ActorSystem)
  extends CustomExecutionContext(system, "my.executor") with MyExecutionContext
