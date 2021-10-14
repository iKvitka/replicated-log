import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

class PrivateRoutes(inMemoryStorage: InMemoryStorage) {
  val route: Route = pathPrefix("secondary") {
    path("show") {
      post {
        entity(as[String]) { data =>
          onSuccess(inMemoryStorage.store(data)) { _ =>
            complete(StatusCodes.OK)
          }
        }
      }
    }

  }
}