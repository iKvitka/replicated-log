import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import play.api.libs.json.JsValue

class PrivateRoutes(inMemoryStorage: InMemoryStorage) {
  val route: Route =
    path("store") {
      post {
        entity(as[String]) { data =>
          inMemoryStorage.store(data)
          complete(StatusCodes.OK)
        }
      }
    }
}
