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

class InMemoryStorage(replicator: Replicator)(implicit actorSystem: ActorSystem[_], executionContext: ExecutionContextExecutor) extends LazyLogging {

  val data: mutable.SortedMap[Int, String] = mutable.SortedMap.empty
  var counter: AtomicInteger               = new AtomicInteger(0)

  def store(message: LogReplicate): HttpResponse = {
    val id = counter.getAndIncrement()
    data += id -> message.data

    replicator.tryToReplicate(id, message.writeConcern, message.data)
  }

  def showData: String = data.values.mkString("\n")
}