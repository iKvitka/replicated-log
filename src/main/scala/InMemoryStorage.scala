import akka.actor.typed.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json

import scala.collection.mutable
import scala.concurrent.ExecutionContextExecutor

class InMemoryStorage(implicit actorSystem: ActorSystem[_], executionContext: ExecutionContextExecutor) extends LazyLogging {
  val data: mutable.SortedMap[Int, String] = mutable.SortedMap.empty
  var counter: Int                         = 0

  def store(message: String): Unit = {
    val jsonData = Json.parse(message)
    val id       = (jsonData \ "id").as[Int]
    val newData  = (jsonData \ "data").as[String]

    data += id -> newData
    logger.info("json is {} id is {} and data is {}", message, id, newData)
  }

  def showData: String = data.values.mkString("\n")
}
