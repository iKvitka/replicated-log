import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}

class InMemoryStorage(implicit executionContext: ExecutionContextExecutor) {
  val data: mutable.SortedMap[Int, String] = mutable.SortedMap.empty
  var counter: Int                         = 0

  def store(newData: String): Future[mutable.SortedMap[Int, String]] = Future {
    data += counter -> newData
    counter += 1
    data
  }
}
