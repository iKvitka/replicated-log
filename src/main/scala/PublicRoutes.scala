import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

trait PublicRoutes {
  val route: Route
}

class MasterPublicRoutes(inMemoryStorage: InMemoryStorageMaster) extends PublicRoutes {
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

class SecondaryPublicRoutes(inMemoryStorage: InMemoryStorageSecondary) extends PublicRoutes {
  val route: Route = path("data") {
    get {
      complete(inMemoryStorage.showData)
    }
  }
}
