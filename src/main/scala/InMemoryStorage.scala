import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpMethods, HttpRequest, Uri}

import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}

class InMemoryStorage(implicit actorSystem: ActorSystem[_], executionContext: ExecutionContextExecutor) {
  val data: mutable.SortedMap[Int, String] = mutable.SortedMap.empty
  var counter: Int                         = 0

  //Uri for secondaries (name explanation https://www.youtube.com/watch?v=dQw4w9WgXcQ)
  val RickUri: Uri  = Uri("localhost/data:1861")
  val AshlyUri: Uri = Uri("localhost/data:1862")

  def store(newData: String): Future[mutable.SortedMap[Int, String]] =
    for {
      data <- Future {
               data += counter -> newData
               counter += 1
               data
             }
      _ <- Http().singleRequest(
            HttpRequest(
              method = HttpMethods.POST,
              uri = RickUri,
              entity = HttpEntity(newData)
            ))
      _ <- Http().singleRequest(
            HttpRequest(
              method = HttpMethods.POST,
              uri = AshlyUri,
              entity = HttpEntity(newData)
            ))
    } yield data
}