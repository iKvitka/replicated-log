package secondary

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class InMemoryStorage(implicit actorSystem: ActorSystem[_], executionContext: ExecutionContextExecutor) extends LazyLogging {
  val data: mutable.SortedMap[Int, String] = mutable.SortedMap.empty
  var delay: Int                           = 0

  def store(message: String): Unit = {
    Thread.sleep(delay)

    val jsonData = Json.parse(message)
    val id       = (jsonData \ "id").as[Int]
    val newData  = (jsonData \ "data").as[String]

    data += id -> newData
    logger.info("json is {} id is {} and data is {}", message, id, newData)
  }

  //TODO: better to change storing new messages in temporary buffer like in FIFO Replication than this O(4n) crap
  def showData: String = data.zipWithIndex.takeWhile(d => d._1._1 == d._2).map(_._1._2).mkString("\n")

  def setDelay(d: Int): Unit = delay = d
}
