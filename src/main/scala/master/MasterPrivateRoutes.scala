package master

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import common.PrivateRoutes

class MasterPrivateRoutes(inMemoryStorage: InMemoryStorage) extends PrivateRoutes {
  val route: Route = path("secondary") {
    concat(
      post {
        entity(as[String]) { data =>
          inMemoryStorage.addSecondary(data)
          complete(StatusCodes.OK)
        }
      },
      get {
        complete(inMemoryStorage.secondaries.mkString("\n"))
      },
      delete {
        entity(as[String]) { secondary =>
          inMemoryStorage.removeSecondary(secondary)
          complete(StatusCodes.OK)
        }
      }
    )

  }
}

