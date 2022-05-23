package ca.stevenskelton.tinyakkaslackqueue

import akka.Done
import akka.actor.Cancellable
import org.slf4j.Logger

import java.time._
import java.util
import java.util.{Date, Timer, TimerTask, UUID}
import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

abstract class UUIDTask extends Cancellable {
  val uuid = UUID.randomUUID()

  private var shouldCancel: Boolean = false

  override def isCancelled: Boolean = shouldCancel

  override def cancel(): Boolean = {
    if (shouldCancel) false
    else {
      shouldCancel = true
      true
    }
  }

  def run(logger: Logger): Unit
}

class InteractiveJavaUtilTimer[T <: UUIDTask](baseLogger: Logger) {

  protected def createLogger(uuid: UUID): Logger = baseLogger

  protected def humanReadableFormat(duration: Duration): String = {
    duration.toString.substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase
  }

  private def humanReadableTimeFromStart(starttime: Long): String = {
    humanReadableFormat(Duration.ofMillis(System.currentTimeMillis - starttime))
  }

  private class InnerTimerTask(val task: T, onComplete: Try[Done] => Unit) extends TimerTask {

    var (isRunning, isComplete, hasFailed) = (false, false, false)

    override def cancel: Boolean = {
      task.cancel
      super.cancel()
    }

    override def run: Unit = if (!task.isCancelled) {
      isRunning = true
      val logger = createLogger(task.uuid)
      val starttime = System.currentTimeMillis
      val result = try {
        task.run(logger)
        isComplete = true
        Success(Done)
      } catch {
        case NonFatal(ex) =>
          hasFailed = true
          logger.error(s"Job failed after ${humanReadableTimeFromStart(starttime)}", ex)
          Failure(ex)
      } finally {
        isRunning = false
        allTimerTasks.remove(this)
      }
      onComplete(result)
    }
  }

  case class ScheduledTask(task: T, executionStart: ZonedDateTime, isRunning: Boolean) {
    val uuid: UUID = task.uuid
  }

  private def toScheduledTask(innerTimerTask: InnerTimerTask): ScheduledTask = {
    ScheduledTask(
      innerTimerTask.task,
      ZonedDateTime.ofInstant(Instant.ofEpochMilli(innerTimerTask.scheduledExecutionTime), ZoneId.systemDefault()),
      innerTimerTask.isRunning
    )
  }

  private val timer = new Timer(true)

  private val allTimerTasks = java.util.Collections.newSetFromMap[InnerTimerTask](new util.WeakHashMap[InnerTimerTask, java.lang.Boolean]())

  def schedule(task: T, onComplete: Try[Done] => Unit): ScheduledTask = {
    val innerTimerTask = new InnerTimerTask(task, onComplete)
    allTimerTasks.add(innerTimerTask)
    timer.schedule(innerTimerTask, 0)
    toScheduledTask(allTimerTasks.asScala.find(_.task == task).get)
  }

  def schedule(task: T, time: ZonedDateTime, onComplete: Try[Done] => Unit): ScheduledTask = {
    val innerTimerTask = new InnerTimerTask(task, onComplete)
    allTimerTasks.add(innerTimerTask)
    timer.schedule(innerTimerTask, Date.from(time.toInstant))
    toScheduledTask(allTimerTasks.asScala.find(_.task == task).get)
  }

  def list: Seq[ScheduledTask] = {
    allTimerTasks.asScala.flatMap {
      innerTimerTask =>
        val isCancelled = innerTimerTask.task.isCancelled && !innerTimerTask.isRunning
        if (isCancelled || innerTimerTask.isComplete || innerTimerTask.hasFailed) None
        else Some(toScheduledTask(innerTimerTask))
    }.toSeq.sortBy(o => (!o.isRunning, o.executionStart.toInstant))
  }

  def cancel(uuid: UUID): Option[ScheduledTask] = {
    val it = allTimerTasks.iterator
    while (it.hasNext) {
      val attrTimerTask = it.next
      if (attrTimerTask.task.uuid == uuid) {
        attrTimerTask.cancel
        return Some(toScheduledTask(attrTimerTask))
      }
    }
    None
  }

  def cancel(): Boolean = {
    val it = allTimerTasks.iterator
    if (it.hasNext) {
      while (it.hasNext) it.next.cancel
      timer.purge
      true
    } else {
      false
    }
  }

}
