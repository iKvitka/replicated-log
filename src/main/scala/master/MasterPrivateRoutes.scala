package master

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import common.PrivateRoutes

class MasterPrivateRoutes(replicator: Replicator) extends PrivateRoutes {
  val route: Route = path("secondary") {
    concat(
      post {
        entity(as[String]) { data =>
          replicator.addSecondary(data)
          complete(StatusCodes.OK)
        }
      },
      get {
        complete(replicator.showSecondaries.mkString("\n"))
      },
      delete {
        entity(as[String]) { secondary =>
          replicator.removeSecondary(secondary)
          complete(StatusCodes.OK)
        }
      }
    )

  }
}

