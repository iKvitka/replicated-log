import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http

import scala.util.{Failure, Success}

class Server(applicationRoutes: ApplicationRoutes) {
  def start(port: Int)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext
    lazy val config = com.typesafe.config.ConfigFactory.load()
    val bindAddress = config.getString("application.bindAddress")
    val bindPort = config.getInt("application.bindPort")

    val futureBinding = Http().newServerAt(bindAddress, bindPort).bind(applicationRoutes.routes)

    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
}
