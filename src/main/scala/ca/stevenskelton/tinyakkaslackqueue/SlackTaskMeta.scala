package ca.stevenskelton.tinyakkaslackqueue

import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackTaskFactory}
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory._
import org.slf4j.Logger
import play.api.libs.json.OFormat

import java.time.ZonedDateTime
import scala.collection.SortedSet
import scala.jdk.CollectionConverters.ListHasAsScala

object SlackTaskMeta {
  def initialize(slackClient: SlackClient, taskChannel: SlackChannel, historyThread: SlackHistoryThread, factory: SlackTaskFactory[_, _])(implicit logger: Logger): SlackTaskMeta = {
    val response = slackClient.threadReplies(historyThread)
    val executedTasks = scala.collection.mutable.SortedSet.empty[TaskHistoryItem[TaskHistoryOutcomeItem]]
    if (response.isOk) {
      response.getMessages.asScala.withFilter(_.getParentUserId == slackClient.botUserId.value).foreach {
        message =>
          TaskHistoryItem.fromHistoryThreadMessage(message) match {
            case Some(taskHistoryItem) if taskHistoryItem.action.isInstanceOf[TaskHistoryOutcomeItem] =>
              executedTasks.add(taskHistoryItem.asInstanceOf[TaskHistoryItem[TaskHistoryOutcomeItem]])
            case _ =>
          }
      }
    }
    new SlackTaskMeta(slackClient, taskChannel, historyThread, factory, executedTasks)
  }
}

class SlackTaskMeta private(
                             slackClient: SlackClient,
                             val taskChannel: SlackChannel,
                             val historyThread: SlackHistoryThread,
                             val factory: SlackTaskFactory[_, _],
                             executedTasks: scala.collection.mutable.SortedSet[TaskHistoryItem[TaskHistoryOutcomeItem]]
                           )(implicit logger: Logger) {

  implicit val ordering = new Ordering[ScheduledSlackTask] {
    override def compare(x: ScheduledSlackTask, y: ScheduledSlackTask): Int = x.executionStart.compareTo(y.executionStart)
  }

  private def post[T<: TaskHistoryActionItem](taskHistoryItem: TaskHistoryItem[T]): Unit = {
    slackClient.chatPostMessageInThread(taskHistoryItem.toHistoryThreadMessage, historyThread)
    slackClient.chatPostMessageInThread(taskHistoryItem.toTaskThreadMessage, taskHistoryItem.taskId)
  }

  def historyAddCreate(scheduledSlackTask: ScheduledSlackTask): Unit = {
    val taskHistoryOutcome = TaskHistoryItem(
      CreateHistoryItem(scheduledSlackTask.task.createdBy),
      scheduledSlackTask.task.slackTaskThread,
      historyThread,
      ZonedDateTime.now()
    )
    post(taskHistoryOutcome)
  }

  def historyAddRun(ts: SlackTaskThread, estimatedCount: Int): Unit = {
    val taskHistoryOutcome = TaskHistoryItem(
      RunHistoryItem(estimatedCount),
      ts,
      historyThread,
      ZonedDateTime.now()
    )
    post(taskHistoryOutcome)
  }

  def historyAddOutcome[T <: TaskHistoryOutcomeItem](taskHistoryOutcomeItem: T, ts: SlackTaskThread)(implicit fmt: OFormat[T]): Unit = {
    val taskHistoryOutcome = TaskHistoryItem(
      taskHistoryOutcomeItem,
      ts,
      historyThread,
      ZonedDateTime.now()
    )
    executedTasks.add(taskHistoryOutcome)
    post(taskHistoryOutcome)
  }

  private var cancel: Option[TaskHistoryItem[CancelHistoryItem]] = None

  def historyCancel(ts: SlackTaskThread, slackUserId: SlackUserId, currentCount: Int): Unit = {
    val taskHistoryOutcome = TaskHistoryItem(
      CancelHistoryItem(slackUserId, currentCount),
      ts,
      historyThread,
      ZonedDateTime.now()
    )
    cancel = Some(taskHistoryOutcome)
    post(taskHistoryOutcome)
  }

  def history(allQueuedTasks: Seq[ScheduledSlackTask]): TaskHistory = {
    var runningTask: Option[(ScheduledSlackTask, Option[TaskHistoryItem[CancelHistoryItem]])] = None
    val cueTasks = allQueuedTasks.withFilter(_.task.meta.historyThread == historyThread).flatMap {
      scheduleTask =>
        if (scheduleTask.isRunning) {
          runningTask = Some((scheduleTask, cancel.filter(_.taskId.ts == scheduleTask.id)))
          None
        } else {
          Some(scheduleTask)
        }
    }
    TaskHistory(this, runningTask, executed = executedTasks, pending = SortedSet.from(cueTasks))
  }

}
