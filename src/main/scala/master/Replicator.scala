package master

import akka.actor.Scheduler
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import common.CountDownLatch

import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.Success

class Replicator(implicit actorSystem: ActorSystem[_], executionContext: ExecutionContextExecutor, scheduler: Scheduler) {
  val secondaries: mutable.Map[String, PhiAccrualFailureDetector] = mutable.Map.empty

  def addSecondary(address: String): mutable.Map[String, PhiAccrualFailureDetector] =
    secondaries += address -> new PhiAccrualFailureDetector(12, 42, 5.second, 2.second, 1.second)

  def removeSecondary(address: String): mutable.Map[String, PhiAccrualFailureDetector] =
    secondaries -= address

  def showSecondaries: mutable.Iterable[String] =
    secondaries.map(secondary => s"${secondary._1} -> ${heartbeatStatus(secondary._2.phi)}")

  private def heartbeatStatus(phi: Double) = if (phi < 1) "Healthy" else if (phi < 10) "Suspected" else "Unhealthy"

  def startFailureDetection: Future[Unit] =
    Future {
      while (true) {
        secondaries.foreach { heartbeat =>
          Http()
            .singleRequest(
              HttpRequest(
                method = HttpMethods.GET,
                uri = Uri(s"http://${heartbeat._1}/private/health")
              ))
            .onComplete {
              case Success(_) => heartbeat._2.heartbeat()
              case _          =>
            }
        }
        Thread.sleep(1000)
      }
    }

  def checkQuorum: Boolean =
    secondaries.count(secondaries => heartbeatStatus(secondaries._2.phi) == "Healthy") + 1 > secondaries.size / 2

  def tryToReplicate(id: Int, writeConcern: Int, message: String): HttpResponse = {
    def createRequestToSecondary(secondary: String): Future[HttpResponse] =
      akka.pattern.retry(
        attempt = () =>
          Http().singleRequest(
            HttpRequest(
              method = HttpMethods.POST,
              uri = Uri(s"http://${secondary}/private/store"),
              entity = HttpEntity(ContentTypes.`application/json`, s"""{"id": $id, "data": "$message"}""")
            )),
        attempts = 31337,
        minBackoff = 1.second,
        maxBackoff = 64.second,
        randomFactor = 0.2
      )

    val consumersStarted  = new CountDownLatch(writeConcern)
    val mainThreadProceed = new CountDownLatch(1)
    val consumersFinished = new CountDownLatch(writeConcern)
    val s                 = new AtomicInteger(0)

    val response: Iterable[Future[HttpResponse]] = secondaries.keys.map(createRequestToSecondary)

    response.map { request =>
      for {
        _ <- request
        _ = consumersStarted.countDown()
        _ <- mainThreadProceed.await(Duration.Inf)
      } yield {
        s.incrementAndGet()
        consumersFinished.countDown()
      }
    }

    val await = for {
      _ <- consumersStarted.await(Duration.Inf)
      _ = mainThreadProceed.countDown()
      _ <- consumersFinished.await(100000.millis)
    } yield ()
    Await.result(await, Duration.Inf)

    if (s.get() >= writeConcern) HttpResponse(StatusCodes.OK)
    else HttpResponse(StatusCodes.InternalServerError, entity = "Could not replicate data to Secondaries")
  }
}
