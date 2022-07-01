package ca.stevenskelton.tinyakkaslackqueue

import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackTaskFactory}
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory._
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.conversations.ConversationsListRequest
import com.slack.api.methods.request.pins.{PinsAddRequest, PinsListRequest}
import com.slack.api.model.ConversationType
import org.slf4j.Logger
import play.api.libs.json.OFormat

import java.time.ZonedDateTime
import scala.collection.SortedSet
import scala.jdk.CollectionConverters.ListHasAsScala

object SlackTaskMeta {

  def initializeNewChannel(id: Int, slackClient: SlackClient, taskChannel: TaskLogChannel, factory: SlackTaskFactory[_, _])(implicit logger: Logger): SlackTaskMeta = {
    val conversationsResult = slackClient.client.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).channel(taskChannel.id))
    val existing = conversationsResult.getItems.asScala.find(o => o.getCreatedBy == slackClient.slackConfig.botUserId.value && o.getMessage.getText.startsWith(SlackFactories.HistoryThreadHeader))
    existing.map {
      pinsListResponse =>
        val historyThread = SlackQueueThread(pinsListResponse.getMessage, slackClient.slackConfig.botChannel)
        readHistory(id, slackClient, taskChannel, historyThread, factory)
    }.getOrElse {
      val queueThreadResult = slackClient.client.chatPostMessage((r:ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).channel(slackClient.slackConfig.botChannel.id).text(SlackFactories.HistoryThreadHeader))
      val historyThread = SlackQueueThread(queueThreadResult.getMessage, slackClient.slackConfig.botChannel)
      slackClient.client.pinsAdd((r: PinsAddRequest.PinsAddRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).channel(historyThread.channel.id).timestamp(historyThread.ts.value))
      new SlackTaskMeta(id, slackClient, taskChannel, historyThread, factory, scala.collection.mutable.SortedSet.empty)
    }
  }

  def readHistory(id: Int, slackClient: SlackClient, taskChannel: TaskLogChannel, historyThread: SlackQueueThread, factory: SlackTaskFactory[_, _])(implicit logger: Logger): SlackTaskMeta = {
    val response = slackClient.threadReplies(historyThread)
    val executedTasks = scala.collection.mutable.SortedSet.empty[TaskHistoryItem[TaskHistoryOutcomeItem]]
    if (response.isOk) {
      response.getMessages.asScala.withFilter(_.getParentUserId == slackClient.slackConfig.botUserId.value).foreach {
        message =>
          TaskHistoryItem.fromHistoryThreadMessage(message, taskChannel, historyThread) match {
            case Some(taskHistoryItem) if taskHistoryItem.action.isInstanceOf[TaskHistoryOutcomeItem] =>
              executedTasks.add(taskHistoryItem.asInstanceOf[TaskHistoryItem[TaskHistoryOutcomeItem]])
            case _ =>
          }
      }
    } else {
      if (response.getError == "missing_scope") {
        logger.error(s"SlackTaskMeta.initialize missing permissions: ${response.getNeeded}")
      } else {
        logger.error(s"SlackTaskMeta.initialize failed: ${response.getError}")
      }
    }
    new SlackTaskMeta(id, slackClient, taskChannel, historyThread, factory, executedTasks)
  }

}

class SlackTaskMeta private(
                             val index: Int,
                             val slackClient: SlackClient,
                             val taskLogChannel: TaskLogChannel,
                             val historyThread: SlackQueueThread,
                             val factory: SlackTaskFactory[_, _],
                             executedTasks: scala.collection.mutable.SortedSet[TaskHistoryItem[TaskHistoryOutcomeItem]]
                           ) {

  implicit val ordering = new Ordering[ScheduledSlackTask] {
    override def compare(x: ScheduledSlackTask, y: ScheduledSlackTask): Int = x.executionStart.compareTo(y.executionStart)
  }

  private def post[T <: TaskHistoryActionItem](taskHistoryItem: TaskHistoryItem[T]): Unit = {
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
