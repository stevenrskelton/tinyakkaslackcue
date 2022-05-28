package ca.stevenskelton.tinyakkaslackqueue.lib

import ca.stevenskelton.tinyakkaslackqueue.blocks.TaskHistory
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.{TaskHistoryItem, TaskHistoryOutcomeItem}
import ca.stevenskelton.tinyakkaslackqueue.{ScheduledSlackTask, SlackChannel, SlackTs, TinySlackQueue}

import scala.collection.SortedSet

case class SlackTaskMeta(channel: SlackChannel, historyThread: SlackTs, factory: SlackTaskFactory[_,_]) {

  implicit val ordering = new Ordering[ScheduledSlackTask] {
    override def compare(x: ScheduledSlackTask, y: ScheduledSlackTask): Int = x.executionStart.compareTo(y.executionStart)
  }

  def historyAdd(taskHistoryItem: TaskHistoryItem[TaskHistoryOutcomeItem]): Boolean = {
    executedTasks.add(taskHistoryItem)
  }

  private val executedTasks: scala.collection.mutable.SortedSet[TaskHistoryItem[TaskHistoryOutcomeItem]] = {
        //TODO: read Slack thread
        scala.collection.mutable.SortedSet.empty[TaskHistoryItem[TaskHistoryOutcomeItem]]
  }

  def history(tinySlackQueue: TinySlackQueue): TaskHistory = {
    val allQueuedTasks = tinySlackQueue.listScheduledTasks
      var runningTask: Option[ScheduledSlackTask] = None
      val cueTasks = allQueuedTasks.withFilter(_.task.meta.historyThread == historyThread).flatMap {
        scheduleTask =>
          if (scheduleTask.isRunning) {
            runningTask = Some(scheduleTask)
            None
          } else {
            Some(scheduleTask)
          }
      }
      TaskHistory(this, runningTask, executed = executedTasks, pending = SortedSet.from(cueTasks))
  }

}
