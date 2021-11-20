import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

trait PrivateRoutes {
  val route: Route
}

class MasterPrivateRoutes(inMemoryStorage: InMemoryStorageMaster) extends PrivateRoutes {
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

class SecondaryPrivateRoutes(inMemoryStorage: InMemoryStorageSecondary) extends PrivateRoutes {
  val route: Route =
    concat (
      path("store") {
        post {
          entity(as[String]) { data =>
            inMemoryStorage.store(data)
            complete(StatusCodes.OK)
          }
        }
      },
      path("delay"){
        post{
          entity(as[String]){delay =>
            inMemoryStorage.setDelay(delay.toIntOption.getOrElse(0))
            complete(StatusCodes.OK)
          }
        }
      }
    )
}
