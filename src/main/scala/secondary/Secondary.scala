package secondary

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.scalalogging.LazyLogging
import common.{ApplicationRoutes, Server}

import scala.concurrent.ExecutionContextExecutor

object Secondary extends App with LazyLogging {

  val rootBehavior = Behaviors.setup[Nothing] { context =>

    implicit val actorSystem: ActorSystem[Nothing] = context.system
    implicit val executionContext: ExecutionContextExecutor = context.system.executionContext

    val inMemoryStorage = new InMemoryStorage
    val privateRoutes = new SecondaryPrivateRoutes(inMemoryStorage)
    val publicRoutes = new SecondaryPublicRoutes(inMemoryStorage)
    val applicationRoutes = new ApplicationRoutes(privateRoutes, publicRoutes)

    new Server(applicationRoutes).start
    Behaviors.empty
  }

  ActorSystem[Nothing](rootBehavior, "ReplicatedLog")

}
