package master

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import common.PublicRoutes


class MasterPublicRoutes(inMemoryStorage: InMemoryStorage) extends PublicRoutes{
  val route: Route = path("data") {
    concat(
      get {
        complete(inMemoryStorage.showData)
      },
      post {
        import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
        entity(as[LogReplicate]) { data =>
          complete(inMemoryStorage.store(data))
        }
      }
    )
  }
}