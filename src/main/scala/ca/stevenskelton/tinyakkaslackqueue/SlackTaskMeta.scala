package ca.stevenskelton.tinyakkaslackqueue

import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackTaskFactory}
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory._
import ca.stevenskelton.tinyakkaslackqueue.logging.SlackResponseException
import org.slf4j.Logger
import play.api.libs.json.OFormat

import java.time.ZonedDateTime
import scala.collection.SortedSet
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.{Failure, Success, Try}

object SlackTaskMeta {

  def singleAppRunSlackDisabled(slackClient: SlackClient, factory: SlackTaskFactory[_, _]): SlackTaskMeta = {
    new SlackTaskMeta(0, slackClient, TaskLogChannel(""), SlackQueueThread(SlackTs.Empty, BotChannel("")), factory, scala.collection.mutable.SortedSet.empty)
  }

  def skipHistory(index: Int, slackClient: SlackClient, taskChannel: TaskLogChannel, queueThread: SlackQueueThread, factory: SlackTaskFactory[_, _])(implicit logger: Logger): SlackTaskMeta = {
    new SlackTaskMeta(index, slackClient, taskChannel, queueThread, factory, scala.collection.mutable.SortedSet.empty)
  }

  def readHistory(id: Int, slackClient: SlackClient, taskChannel: TaskLogChannel, queueThread: SlackQueueThread, factory: SlackTaskFactory[_, _])(implicit logger: Logger): Option[SlackTaskMeta] = {
    slackClient.threadReplies(queueThread).map {
      response =>
        val executedTasks = scala.collection.mutable.SortedSet.empty[TaskHistoryItem[TaskHistoryOutcomeItem]]
        if (response.isOk) {
          response.getMessages.asScala.withFilter(_.getParentUserId == slackClient.slackConfig.botUserId.value).foreach {
            message =>
              TaskHistoryItem.fromHistoryThreadMessage(message, taskChannel, queueThread) match {
                case Some(taskHistoryItem) if taskHistoryItem.action.isInstanceOf[TaskHistoryOutcomeItem] =>
                  executedTasks.add(taskHistoryItem.asInstanceOf[TaskHistoryItem[TaskHistoryOutcomeItem]])
                case _ =>
              }
          }
          Some(new SlackTaskMeta(id, slackClient, taskChannel, queueThread, factory, executedTasks))
        } else {
          if (response.getError == "missing_scope") {
            logger.error(s"SlackTaskMeta.initialize missing permissions: ${response.getNeeded}")
          } else {
            logger.error(s"SlackTaskMeta.initialize failed: ${response.getError}")
          }
          None
        }
    }.getOrElse {
      None
    }
  }

}

class SlackTaskMeta private(
                             val index: Int,
                             val slackClient: SlackClient,
                             val taskLogChannel: TaskLogChannel,
                             val queueThread: SlackQueueThread,
                             val factory: SlackTaskFactory[_, _],
                             executedTasks: scala.collection.mutable.SortedSet[TaskHistoryItem[TaskHistoryOutcomeItem]]
                           ) {

  implicit val ordering = new Ordering[ScheduledSlackTask] {
    override def compare(x: ScheduledSlackTask, y: ScheduledSlackTask): Int = x.executionStart.compareTo(y.executionStart)
  }

  private def post[T <: TaskHistoryActionItem](taskHistoryItem: TaskHistoryItem[T]): Try[Unit] = {
    slackClient.chatPostMessageInThread(taskHistoryItem.toHistoryThreadMessage, queueThread).flatMap {
      response1 =>
        if (response1.isOk) {
          slackClient.chatPostMessageInThread(taskHistoryItem.toTaskThreadMessage, taskHistoryItem.taskId).flatMap {
            response2 =>
              if (response2.isOk) {
                Success(())
              } else {
                Failure(SlackResponseException(response2))
              }
          }
        } else {
          Failure(SlackResponseException(response1))
        }
    }
  }

  def updateTaskLogChannel(channel: TaskLogChannel): SlackTaskMeta = new SlackTaskMeta(index, slackClient, channel, queueThread, factory, executedTasks)

  def historyAddCreate(scheduledSlackTask: ScheduledSlackTask): Try[Unit] = {
    val taskHistoryOutcome = TaskHistoryItem(
      CreateHistoryItem(scheduledSlackTask.task.createdBy),
      scheduledSlackTask.task.slackTaskThread,
      queueThread,
      ZonedDateTime.now()
    )
    post(taskHistoryOutcome)
  }

  def historyAddRun(ts: SlackTaskThread, estimatedCount: Int): Try[Unit] = {
    val taskHistoryOutcome = TaskHistoryItem(
      RunHistoryItem(estimatedCount),
      ts,
      queueThread,
      ZonedDateTime.now()
    )
    post(taskHistoryOutcome)
  }

  def historyAddOutcome[T <: TaskHistoryOutcomeItem](taskHistoryOutcomeItem: T, ts: SlackTaskThread)(implicit fmt: OFormat[T]): Try[Unit] = {
    val taskHistoryOutcome = TaskHistoryItem(
      taskHistoryOutcomeItem,
      ts,
      queueThread,
      ZonedDateTime.now()
    )
    executedTasks.add(taskHistoryOutcome)
    post(taskHistoryOutcome)
  }

  private var cancel: Option[TaskHistoryItem[CancelHistoryItem]] = None

  def historyCancel(ts: SlackTaskThread, slackUserId: SlackUserId, currentCount: Int): Try[Unit] = {
    val taskHistoryOutcome = TaskHistoryItem(
      CancelHistoryItem(slackUserId, currentCount),
      ts,
      queueThread,
      ZonedDateTime.now()
    )
    cancel = Some(taskHistoryOutcome)
    post(taskHistoryOutcome)
  }

  def history(allQueuedTasks: Seq[ScheduledSlackTask]): TaskHistory = {
    var runningTask: Option[(ScheduledSlackTask, Option[TaskHistoryItem[CancelHistoryItem]])] = None
    val cueTasks = allQueuedTasks.withFilter(_.task.meta.queueThread == queueThread).flatMap {
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
