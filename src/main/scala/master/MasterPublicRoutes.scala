package master

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import common.PublicRoutes
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

class MasterPublicRoutes(inMemoryStorage: InMemoryStorage) extends PublicRoutes{
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