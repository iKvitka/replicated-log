import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}

class InMemoryStorage(implicit actorSystem: ActorSystem[_], executionContext: ExecutionContextExecutor) extends LazyLogging{
  val secondaries: mutable.ListBuffer[Uri] = mutable.ListBuffer.empty

  val data: mutable.SortedMap[Int, String] = mutable.SortedMap.empty
  var counter: Int                         = 0

  def addSecondary(uri: String): mutable.ListBuffer[Uri] =
    secondaries += Uri(uri)
  //TODO: add sending data to new secondary

  def store(newData: String): Unit = {
    def createRequestToSecondary(uri: Uri): Future[HttpResponse] =
      Http().singleRequest(
        HttpRequest(
          method = HttpMethods.POST,
          uri = uri,
          entity = HttpEntity(ContentTypes.`application/json`, s"""{"id": $counter, "data": $newData}""")
        ))

    Future.sequence(secondaries.map(createRequestToSecondary))

    data += counter -> newData
    counter += 1
  }
}
