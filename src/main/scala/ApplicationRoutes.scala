import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

class ApplicationRoutes(inMemoryStorage: InMemoryStorage) {

  val routes: Route =
    path("data") {
      concat(
        get {
          complete(inMemoryStorage.data.mkString("\n"))
        })

      path("internal") {
          concat {
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
}
