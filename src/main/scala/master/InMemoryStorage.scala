package master

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class InMemoryStorage(replicator: Replicator)(implicit actorSystem: ActorSystem[_],
                                              executionContext: ExecutionContextExecutor)
    extends LazyLogging {

  val data: mutable.SortedMap[Int, String] = mutable.SortedMap.empty
  var counter: AtomicInteger               = new AtomicInteger(0)

  def store(message: LogReplicate): HttpResponse =
    if (replicator.checkQuorum) {
      val id = counter.getAndIncrement()
      data += id -> message.data

      replicator.tryToReplicate(id, message.writeConcern-1, message.data)
    }
    else HttpResponse(StatusCodes.InternalServerError, entity = "There are no quorum cluster in read only mode")


  def showData: String = data.values.mkString("\n")
}
