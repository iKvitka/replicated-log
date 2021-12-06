package master

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class Replicator(implicit actorSystem: ActorSystem[_], executionContext: ExecutionContextExecutor) {
  val secondaries: mutable.Set[String] = mutable.Set.empty

  def addSecondary(address: String): mutable.Set[String] =
    secondaries += address

  def removeSecondary(address: String): mutable.Set[String] =
    secondaries -= address

  def tryToReplicate(id: Int, writeConcern:Int, message: String): HttpResponse = {
    def createRequestToSecondary(secondary: String): Future[HttpResponse] =
      Http().singleRequest(
        HttpRequest(
          method = HttpMethods.POST,
          uri = Uri(s"http://${secondary}/private/store"),
          entity = HttpEntity(ContentTypes.`application/json`, s"""{"id": $id, "data": "$message"}""")
        ))

    val response: mutable.Set[Future[HttpResponse]] = secondaries.map(createRequestToSecondary)
    val s                                           = new AtomicInteger(0)
    val f                                           = new AtomicInteger(0)
    response.foreach(_.onComplete {
      case Success(_) => s.incrementAndGet()
      case Failure(_) => f.incrementAndGet()
    })

    while (s.get() < writeConcern && secondaries.size - f.get() >= writeConcern) {
      Thread.sleep(50)
    }

    if (s.get() >= writeConcern) HttpResponse(StatusCodes.OK)
    else HttpResponse(StatusCodes.InternalServerError, entity = "Could not replicate data to Secondaries")
  }
}
