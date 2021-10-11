import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

class PublicRoutes(inMemoryStorage: InMemoryStorage) {
  val route: Route = path("data") {
    concat(
      get {
        complete(inMemoryStorage.data.mkString("\n"))
      },
      post {
        entity(as[String]) { data =>
          onSuccess(inMemoryStorage.store(data)) { _ =>
            complete(StatusCodes.OK)
          }
        }
      }
    )
  }
}
