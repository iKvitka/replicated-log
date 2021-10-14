import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class InMemoryStorage(implicit actorSystem: ActorSystem[_], executionContext: ExecutionContextExecutor) extends LazyLogging {
  val secondaries: mutable.Set[String] = mutable.Set.empty

  val data: mutable.SortedMap[Int, String] = mutable.SortedMap.empty
  var counter: Int                         = 0

  def addSecondary(address: String): mutable.Set[String] =
    secondaries += address
  //TODO: add sending data to new secondary

  def store(newData: String): Unit = {
    def createRequestToSecondary(address: String): Future[HttpResponse] =
      Http().singleRequest(
        HttpRequest(
          method = HttpMethods.POST,
          uri = Uri(s"http://$address/private/store"),
          entity = HttpEntity(ContentTypes.`application/json`, s"""{"id": $counter, "data": "$newData"}""")
        ))

    val res = for {
      res <- Future.sequence(secondaries.map(createRequestToSecondary))
      _ = logger.debug("finish sending requests")
    } yield res

    res.onComplete{
      case Success(value) =>
        data += counter -> newData
        counter += 1
        logger.info("data was stored successfully {}", value)
      case Failure(exception) => logger.error("lol {}", exception)
    }


  }

  def showData: String = data.values.mkString("\n")
}
