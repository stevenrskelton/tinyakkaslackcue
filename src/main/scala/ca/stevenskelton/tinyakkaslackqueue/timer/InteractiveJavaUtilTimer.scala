package ca.stevenskelton.tinyakkaslackqueue.timer

import akka.Done

import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util
import java.util.{Date, Timer, TimerTask}
import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class InteractiveJavaUtilTimer[S, T <: IdTask[S]] {

  private class InnerTimerTask(val task: T, onComplete: Try[Done] => Unit) extends TimerTask {

    @volatile var (isRunning, isComplete, hasFailed) = (false, false, false)

    override def cancel: Boolean = {
      task.cancel
      super.cancel()
    }

    override def run: Unit = if (!task.isCancelled) {
      isRunning = true
      val result = try {
        task.run()
        isComplete = true
        Success(Done)
      } catch {
        case NonFatal(ex) =>
          hasFailed = true
          Failure(ex)
      } finally {
        isRunning = false
        allTimerTasks.remove(this)
      }
      onComplete(result)
    }
  }

  case class ScheduledTask(task: T, executionStart: ZonedDateTime, isRunning: Boolean) {
    val id: S = task.id
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

  def isExecuting: Boolean = allTimerTasks.asScala.exists(_.isRunning)

  def cancel(id: S): Option[ScheduledTask] = {
    val it = allTimerTasks.iterator
    while (it.hasNext) {
      val attrTimerTask = it.next
      if (attrTimerTask.task.id == id) {
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
