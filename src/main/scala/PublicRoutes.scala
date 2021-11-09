import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

class PublicRoutes(inMemoryStorage: InMemoryStorage) {
  val route: Route = path("data") {
    concat(
      get {
        complete(inMemoryStorage.showData)
      },
      post {
        entity(as[LogReplicate]) { data =>
          complete(inMemoryStorage.store(data))
        }
      }
    )
  }
}
