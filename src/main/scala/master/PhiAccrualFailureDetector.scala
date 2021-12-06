package master

import master.FailureDetector.Clock

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, FiniteDuration}

class PhiAccrualFailureDetector(threshold: Double,
                                maxSampleSize: Int,
                                minStdDeviation: FiniteDuration,
                                acceptableHeartbeatPause: FiniteDuration,
                                firstHeartbeatEstimate: FiniteDuration)(implicit
                                                                        clock: Clock)
    extends FailureDetector {

  require(threshold > 0.0, "failure-detector.threshold must be > 0")
  require(maxSampleSize > 0, "failure-detector.max-sample-size must be > 0")
  require(minStdDeviation > Duration.Zero, "failure-detector.min-std-deviation must be > 0")
  require(acceptableHeartbeatPause >= Duration.Zero, "failure-detector.acceptable-heartbeat-pause must be >= 0")
  require(firstHeartbeatEstimate > Duration.Zero, "failure-detector.heartbeat-interval must be > 0")


  private val firstHeartbeat: HeartbeatHistory = {
    val mean         = firstHeartbeatEstimate.toMillis
    val stdDeviation = mean / 4
    HeartbeatHistory(maxSampleSize) :+ (mean - stdDeviation) :+ (mean + stdDeviation)
  }

  private val acceptableHeartbeatPauseMillis = acceptableHeartbeatPause.toMillis

  private case class State(history: HeartbeatHistory, timestamp: Option[Long])

  private val state = new AtomicReference[State](State(history = firstHeartbeat, timestamp = None))

  override def isAvailable: Boolean = isAvailable(clock())

  private def isAvailable(timestamp: Long): Boolean = phi(timestamp) < threshold

  override def isMonitoring: Boolean = state.get.timestamp.nonEmpty

  @tailrec
  final override def heartbeat(): Unit = {
    val timestamp = clock()
    val oldState  = state.get

    val newHistory = oldState.timestamp match {
      case None =>
        firstHeartbeat
      case Some(latestTimestamp) =>
        val interval = timestamp - latestTimestamp
        if (isAvailable(timestamp)) {
          oldState.history :+ interval
        } else oldState.history
    }

    val newState = oldState.copy(history = newHistory, timestamp = Some(timestamp))

    if (!state.compareAndSet(oldState, newState)) heartbeat()
  }


  def phi: Double = phi(clock())

  private def phi(timestamp: Long): Double = {
    val oldState     = state.get
    val oldTimestamp = oldState.timestamp

    if (oldTimestamp.isEmpty) 0.0
    else {
      val timeDiff = timestamp - oldTimestamp.get

      val history      = oldState.history
      val mean         = history.mean
      val stdDeviation = ensureValidStdDeviation(history.stdDeviation)

      phi(timeDiff, mean + acceptableHeartbeatPauseMillis, stdDeviation)
    }
  }

  private def phi(timeDiff: Long, mean: Double, stdDeviation: Double): Double = {
    val y = (timeDiff - mean) / stdDeviation
    val e = math.exp(-y * (1.5976 + 0.070566 * y * y))
    if (timeDiff > mean)
      -math.log10(e / (1.0 + e))
    else
      -math.log10(1.0 - 1.0 / (1.0 + e))
  }

  private val minStdDeviationMillis = minStdDeviation.toMillis.toDouble

  private def ensureValidStdDeviation(stdDeviation: Double): Double = math.max(stdDeviation, minStdDeviationMillis)

}

final case class HeartbeatHistory private (maxSampleSize: Int,
                                           intervals: IndexedSeq[Long] = IndexedSeq.empty,
                                           intervalSum: Long = 0L,
                                           squaredIntervalSum: Long = 0L) {

  if (maxSampleSize < 1)
    throw new IllegalArgumentException(s"maxSampleSize must be >= 1, got [$maxSampleSize]")
  if (intervalSum < 0L)
    throw new IllegalArgumentException(s"intervalSum must be >= 0, got [$intervalSum]")
  if (squaredIntervalSum < 0L)
    throw new IllegalArgumentException(s"squaredIntervalSum must be >= 0, got [$squaredIntervalSum]")

  def mean: Double = intervalSum.toDouble / intervals.size

  def variance: Double = (squaredIntervalSum.toDouble / intervals.size) - (mean * mean)

  def stdDeviation: Double = math.sqrt(variance)

  @tailrec
  def :+(interval: Long): HeartbeatHistory =
    if (intervals.size < maxSampleSize)
      HeartbeatHistory(maxSampleSize,
                       intervals = intervals :+ interval,
                       intervalSum = intervalSum + interval,
                       squaredIntervalSum = squaredIntervalSum + pow2(interval))
    else
      dropOldest :+ interval

  private def dropOldest: HeartbeatHistory =
    HeartbeatHistory(maxSampleSize,
                     intervals = intervals.drop(1),
                     intervalSum = intervalSum - intervals.head,
                     squaredIntervalSum = squaredIntervalSum - pow2(intervals.head))

  private def pow2(x: Long) = x * x
}
