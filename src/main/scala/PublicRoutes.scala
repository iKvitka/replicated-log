import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

class PublicRoutes(inMemoryStorage: InMemoryStorage) {
  val route: Route = path("data") {
    get {
      complete(inMemoryStorage.showData)
    }
  }
}
