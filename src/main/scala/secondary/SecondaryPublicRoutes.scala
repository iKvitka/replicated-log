package secondary

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import common.PublicRoutes
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

class SecondaryPublicRoutes(inMemoryStorage: InMemoryStorage) extends PublicRoutes {
  val route: Route = path("data") {
    get {
      complete(inMemoryStorage.showData)
    }
  }
}
