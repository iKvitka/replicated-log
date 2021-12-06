package common

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

trait PrivateRoutes{
  val route: Route
}
trait PublicRoutes{
  val route: Route
}

class ApplicationRoutes(privateRoutes: PrivateRoutes, publicRoutes: PublicRoutes) {

  val routes: Route = {
    concat(
      pathPrefix("private")(privateRoutes.route),
      pathPrefix("public")(publicRoutes.route)
    )
  }

}
