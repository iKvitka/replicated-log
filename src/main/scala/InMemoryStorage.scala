import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, Uri}

import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}

class InMemoryStorage(implicit actorSystem: ActorSystem[_], executionContext: ExecutionContextExecutor) {
  val secondaries: mutable.ListBuffer[Uri] = mutable.ListBuffer.empty

  val data: mutable.SortedMap[Int, String] = mutable.SortedMap.empty
  var counter: Int                         = 0

  def addSecondary(uri: String): Future[mutable.ListBuffer[Uri]] =
    Future(secondaries += Uri(uri))

  def store(newData: String): Future[mutable.SortedMap[Int, String]] = {
    def createRequestToSecondary(uri: Uri): Future[HttpResponse] =
      Http().singleRequest(
        HttpRequest(
          method = HttpMethods.POST,
          uri = uri,
          entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`,newData)
        ))
    for {
      data <- Future {
               data += counter -> newData
               counter += 1
               data
             }
      _ <- Future.sequence(secondaries.map(createRequestToSecondary))

    } yield data
  }
}
