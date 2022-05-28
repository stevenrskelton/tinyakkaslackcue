package ca.stevenskelton.tinyakkaslackqueue

import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackTaskFactory}
import ca.stevenskelton.tinyakkaslackqueue.blocks.TaskHistory
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.{CancelHistoryItem, CreateHistoryItem, RunHistoryItem, TaskHistoryItem, TaskHistoryOutcomeItem}
import org.slf4j.Logger
import play.api.libs.json.OFormat

import java.time.ZonedDateTime
import scala.collection.SortedSet
import scala.jdk.CollectionConverters.ListHasAsScala

object SlackTaskMeta {
  def initialize(slackClient: SlackClient, channel: SlackChannel, historyThread: SlackTs, factory: SlackTaskFactory[_, _])(implicit logger: Logger): SlackTaskMeta = {
    val response = slackClient.threadReplies(historyThread)
    val executedTasks = scala.collection.mutable.SortedSet.empty[TaskHistoryItem[TaskHistoryOutcomeItem]]
    if (response.isOk) {
      response.getMessages.asScala.withFilter(_.getParentUserId == slackClient.botUserId.value).foreach {
        message =>
          TaskHistoryItem.fromMessage(message, historyThread, historyThread, channel) match {
            case Some(taskHistoryItem) if taskHistoryItem.action.isInstanceOf[TaskHistoryOutcomeItem] =>
              executedTasks.add(taskHistoryItem.asInstanceOf[TaskHistoryItem[TaskHistoryOutcomeItem]])
            case _ =>
          }
      }
    }
    new SlackTaskMeta(slackClient, channel, historyThread, factory, executedTasks)
  }
}

class SlackTaskMeta private(
                             slackClient: SlackClient,
                             val channel: SlackChannel,
                             val historyThread: SlackTs,
                             val factory: SlackTaskFactory[_, _],
                             executedTasks: scala.collection.mutable.SortedSet[TaskHistoryItem[TaskHistoryOutcomeItem]]
                           ) {

  implicit val ordering = new Ordering[ScheduledSlackTask] {
    override def compare(x: ScheduledSlackTask, y: ScheduledSlackTask): Int = x.executionStart.compareTo(y.executionStart)
  }

  def historyAddCreate(scheduledSlackTask:ScheduledSlackTask) = {
    val taskHistoryOutcome = TaskHistoryItem(
      CreateHistoryItem(scheduledSlackTask.task.createdBy),
      scheduledSlackTask.id,
      historyThread,
      channel,
      ZonedDateTime.now()
    )
    slackClient.chatPostMessageInThread(taskHistoryOutcome.toSlackMessage, historyThread)
  }

  def historyAddRun( ts: SlackTs, estimatedCount: Int) = {
    val taskHistoryOutcome = TaskHistoryItem(
      RunHistoryItem(estimatedCount),
      ts,
      historyThread,
      channel,
      ZonedDateTime.now()
    )
    slackClient.chatPostMessageInThread(taskHistoryOutcome.toSlackMessage, historyThread)
  }

  def historyAddOutcome[T <: TaskHistoryOutcomeItem](taskHistoryOutcomeItem: T, ts: SlackTs)(implicit fmt: OFormat[T]): Boolean = {
    val taskHistoryOutcome = TaskHistoryItem(
      taskHistoryOutcomeItem,
      ts,
      historyThread,
      channel,
      ZonedDateTime.now()
    )
    slackClient.chatPostMessageInThread(taskHistoryOutcome.toSlackMessage, historyThread)
    executedTasks.add(taskHistoryOutcome)
  }

  private var cancel: Option[TaskHistoryItem[CancelHistoryItem]] = None

  def historyCancel(ts: SlackTs, slackUserId: SlackUserId, currentCount: Int): Unit = {
    val taskHistoryOutcome = TaskHistoryItem(
      CancelHistoryItem(slackUserId, currentCount),
      ts,
      historyThread,
      channel,
      ZonedDateTime.now()
    )
    slackClient.chatPostMessageInThread(taskHistoryOutcome.toSlackMessage, historyThread)
    cancel = Some(taskHistoryOutcome)
  }

  def history(allQueuedTasks: Seq[ScheduledSlackTask]): TaskHistory = {
    var runningTask: Option[(ScheduledSlackTask, Option[TaskHistoryItem[CancelHistoryItem]])] = None
    val cueTasks = allQueuedTasks.withFilter(_.task.meta.historyThread == historyThread).flatMap {
      scheduleTask =>
        if (scheduleTask.isRunning) {
          runningTask = Some((scheduleTask, cancel.filter(_.ts == scheduleTask.id)))
          None
        } else {
          Some(scheduleTask)
        }
    }
    TaskHistory(this, runningTask, executed = executedTasks, pending = SortedSet.from(cueTasks))
  }

}
