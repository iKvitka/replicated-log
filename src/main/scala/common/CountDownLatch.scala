package common

import java.util.concurrent.{Executors, RejectedExecutionException}
import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise, TimeoutException}

final class CountDownLatch(count: Int)(implicit ec: ExecutionContext) extends AutoCloseable {

  // Shared mutable state, managed by my favorite
  // synchronization primitive
  private[this] val state = new AtomicReference(State(count, Set.empty))
  private final case class State(
      count: Int,
      tasks: Set[Promise[Unit]]
  )

  /**
    * INTERNAL — scheduler for managing timeouts, must be destroyed
    * internally too. Note that this isn't the only possible design,
    * we could of course take this as a parameter in the class's
    * constructor, or use a globally shared one 🤷‍
    *
    * One cool alternative is to use Monix's `Scheduler` instead of
    * `ExecutionContext` (`monix.execution.Scheduler`).
    */
  private[this] val scheduler =
    Executors.newSingleThreadScheduledExecutor((r: Runnable) => {
      val th = new Thread(r)
      th.setDaemon(true)
      th.setName(s"countdownlatch-${hashCode()}")
      th
    })

  /**
    * INTERNAL — Given a `Promise`, install a timeout for it.
    */
  private def installTimeout(p: Promise[Unit], timeout: FiniteDuration): Unit = {
    // Removing the promise from our internal `state` is necessary to avoid leaks.
    @tailrec def removePromise(): Unit =
      state.get() match {
        case current @ State(_, tasks) =>
          val update = current.copy(tasks = tasks - p)
          if (!state.compareAndSet(current, update))
            removePromise()
        case null =>
          () // countDown reached zero
      }
    // Keeping this as a value in order to avoid duplicate work
    val ex = new TimeoutException(
      s"AsyncCountDownLatch.await($timeout)"
    )
    val timeoutTask: Runnable = () => {
      // Timeout won, get rid of promise from our state
      removePromise()
      p.tryFailure(ex)
    }
    try {
      // Finally, installing our timeout, w00t!
      val cancelToken = scheduler.schedule(
        timeoutTask,
        timeout.length,
        timeout.unit
      )
      // Canceling our timeout task, if primary completes first
      p.future.onComplete { r =>
        // Avoiding duplicate work
        if (r.fold(_ != ex, _ => true))
          cancelToken.cancel(false)
      }
    } catch {
      case _: RejectedExecutionException =>
        // This exception can happen due to a race condition:
        // When countDown() reaches zero, the scheduler is being
        // shut down, however while that happens we may be in
        // the process of installing a timeout — but this is no
        // longer necessary, since the promise will be completed;
        // NOTE — for extra-safety, we might want to check the
        // promise's state, to ensure that it is complete, and
        // a happens-before relationship probably exists (TBD)
        ()
    }
  }

  override def close(): Unit = {
    state.lazySet(null) // GC purposes
    scheduler.shutdown()
  }

  /**
    * Decrements the count of the latch, releasing all waiting
    * consumers if the count reaches zero.
    *
    * If the current count is already zero, then nothing happens.
    */
  @tailrec def countDown(): Unit =
    state.get() match {
      case current @ State(count, tasks) if count > 0 =>
        val update = State(count - 1, tasks)
        if (!state.compareAndSet(current, update))
          countDown() // retry
        else if (update.count == 0) {
          // Deferring execution to another thread, as it might
          // be expensive (TBD if this is a good idea or not)
          ec.execute(() => {
            for (r <- tasks) r.trySuccess(())
            // Releasing resources
            close()
          })
        }
      case _ =>
        ()
    }

  /**
    * Causes the consumer to wait until the latch has counted down
    * to zero, or the specified waiting time elapses.
    *
    * If the timeout gets triggered, then the returned `Future`
    * will complete with a `TimeoutException`.
    */
  @tailrec def await(timeout: Duration): Future[Unit] =
    state.get() match {
      case current @ State(count, tasks) if count > 0 =>
        val p      = Promise[Unit]()
        val update = State(count, tasks + p)
        timeout match {
          case d: FiniteDuration => installTimeout(p, d)
          case _                 => ()
        }
        if (!state.compareAndSet(current, update))
          await(timeout) // retry
        else
          p.future
      case _ =>
        Future.unit
    }
}
