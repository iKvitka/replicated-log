import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

class PrivateRoutes(inMemoryStorage: InMemoryStorage) {
  val route: Route = pathPrefix("secondary") {
    concat(path("add") {
      post {
        entity(as[String]) { data =>
          onSuccess(inMemoryStorage.addSecondary(data)) { _ =>
            complete(StatusCodes.OK)
          }
        }
      }
    },
      path("show"){
        get{
          complete(inMemoryStorage.secondaries.mkString("\n"))
        }
      }
    )

  }
}
