import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

class PrivateRoutes(inMemoryStorage: InMemoryStorage) {
  val route: Route =
    concat (
      path("store") {
        post {
          entity(as[String]) { data =>
            inMemoryStorage.store(data)
            complete(StatusCodes.OK)
          }
        }
      },
      path("delay"){
        post{
          entity(as[String]){delay =>
            inMemoryStorage.setDelay(delay.toIntOption.getOrElse(0))
            complete(StatusCodes.OK)
          }
        }
      }
    )
}
