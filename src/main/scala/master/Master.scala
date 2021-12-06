package master

import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import com.typesafe.scalalogging.LazyLogging
import common.{ApplicationRoutes, Server}

import scala.concurrent.ExecutionContextExecutor

object Master extends App with LazyLogging {

  val rootBehavior = Behaviors.setup[Nothing] { context =>
    implicit val actorSystem: ActorSystem[Nothing]          = context.system
    implicit val executionContext: ExecutionContextExecutor = context.system.executionContext
    implicit val scheduler: actor.Scheduler                 = actorSystem.scheduler.toClassic

    val replicator        = new Replicator
    replicator.startFailureDetection
    val inMemoryStorage   = new InMemoryStorage(replicator)
    val privateRoutes     = new MasterPrivateRoutes(replicator)
    val publicRoutes      = new MasterPublicRoutes(inMemoryStorage)
    val applicationRoutes = new ApplicationRoutes(privateRoutes, publicRoutes)

    new Server(applicationRoutes).start
    Behaviors.empty
  }

  ActorSystem[Nothing](rootBehavior, "ReplicatedLog")

}
