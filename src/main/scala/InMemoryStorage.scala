import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}

class InMemoryStorage(implicit actorSystem: ActorSystem[_], executionContext: ExecutionContextExecutor) extends LazyLogging {
  val secondaries: mutable.Set[String] = mutable.Set.empty

  val data: mutable.SortedMap[Int, String] = mutable.SortedMap.empty
  var counter: Int                         = 0

  def addSecondary(address: String): mutable.Set[String] =
    secondaries += address
  //TODO: add sending data to new secondary

  def removeSecondary(address: String): mutable.Set[String] =
    secondaries -= address

  def store(newData: String): Future[StatusCode] = {
    def createRequestToSecondary(address: String): Future[HttpResponse] =
      Http().singleRequest(
        HttpRequest(
          method = HttpMethods.POST,
          uri = Uri(s"http://$address/private/store"),
          entity = HttpEntity(ContentTypes.`application/json`, s"""{"id": $counter, "data": "$newData"}""")
        ))

    val storeToSecondaries = Future.sequence(secondaries.map(createRequestToSecondary))

    storeToSecondaries.map { requests =>
      if (requests.forall(_.status == StatusCodes.OK)) {
        data += counter -> newData
        counter += 1
        logger.info("Requests to secondaries succeeded")
        StatusCodes.OK
      } else {
        logger.warn("at least one of secondaries failed to save message")
        StatusCodes.InternalServerError
      }
    }

  }

  def showData: String = data.values.mkString("\n")
}
