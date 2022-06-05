package ca.stevenskelton.tinyakkaslackqueue.api

import akka.Done
import akka.stream.Materializer
import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.blocks.PrivateMetadata
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.TaskHistory
import ca.stevenskelton.tinyakkaslackqueue.timer.InteractiveJavaUtilTimer
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.conversations.{ConversationsCreateRequest, ConversationsListRequest}
import com.slack.api.methods.request.pins.{PinsAddRequest, PinsListRequest}
import com.slack.api.model.ConversationType
import org.slf4j.Logger

import java.time.ZonedDateTime
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import scala.util.{Failure, Success, Try}

abstract class SlackFactories()(implicit val logger: Logger, val slackClient: SlackClient, materializer: Materializer) {

  protected val factories: Seq[SlackTaskFactory[_, _]]

  private val interactiveTimer = new InteractiveJavaUtilTimer[SlackTs, SlackTask]()

  def onComplete(slackTask: SlackTask, result: Try[Done]): Unit = {
    result match {
      case Failure(ex) =>
      case Success(Done) =>
      //        protected def humanReadableFormat(duration: Duration): String = {
      //          duration.toString.substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase
      //        }
      //
      //        private def humanReadableTimeFromStart(starttime: Long): String = {
      //          humanReadableFormat(Duration.ofMillis(System.currentTimeMillis - starttime))
      //        }
    }
  }

  def listScheduledTasks: Seq[ScheduledSlackTask] = interactiveTimer.list

  def isExecuting: Boolean = interactiveTimer.isExecuting

  def cancelScheduledTask(slackTs: SlackTs): Option[ScheduledSlackTask] = interactiveTimer.cancel(slackTs)

  def scheduleSlackTask(slackUserId: SlackUserId, slackTaskMeta: SlackTaskMeta, time: Option[ZonedDateTime]): ScheduledSlackTask = {
    val message = time.map {
      zonedDateTime =>
        s"Scheduled task *${slackTaskMeta.factory.name.getText}* for ${DateUtils.humanReadable(zonedDateTime)}"
    }.getOrElse {
      s"Queued task *${slackTaskMeta.factory.name.getText}*"
    }
    val slackPlaceholder = slackClient.chatPostMessage(message, slackTaskMeta.taskLogChannel)
    val slackTask = slackTaskMeta.factory.create(
      slackTaskMeta,
      taskThread = SlackTaskThread(slackPlaceholder, slackTaskMeta.taskLogChannel),
      createdBy = slackUserId,
      notifyOnError = Nil,
      notifyOnComplete = Nil
    )
    val scheduledTask = time.fold(interactiveTimer.schedule(slackTask, onComplete(slackTask, _)))(interactiveTimer.schedule(slackTask, _, onComplete(slackTask, _)))
    slackTaskMeta.historyAddCreate(scheduledTask)
    scheduledTask
  }

  def history: Seq[TaskHistory] = {
    val allTasks = listScheduledTasks
    slackTaskMetaFactories.map(_.history(allTasks))
  }

  def factoryLogChannels: Seq[(SlackTaskFactory[_,_], Option[TaskLogChannel])] = factories.map {
    factory => factory -> slackClient.slackConfig.taskChannels.find(_._1 == factory.name.getText).map(_._2._1)
  }

  lazy val slackTaskMetaFactories: Seq[SlackTaskMeta] = {
    var hasError = false
    val meta = factories.flatMap {
      factory =>
        slackClient.slackConfig.taskChannels.find(_._1 == factory.name.getText).map {
          case (_, (taskLogChannel, slackHistoryThread)) => SlackTaskMeta.initialize(slackClient, taskLogChannel, slackHistoryThread, factory)
        }.orElse {
          logger.error(s"slackTaskMetaFactories: No config set for task `${factory.name.getText}`")
          hasError = true
          None
        }
    }
    if(hasError) Nil else meta
  }

  def findByChannel(slackChannel: SlackChannel): Try[SlackTaskMeta] = {
    slackTaskMetaFactories.find(_.taskLogChannel == slackChannel).map(Success(_)).getOrElse {
      val ex = new Exception(s"Task ${slackChannel.id} not found")
      logger.error("handleAction", ex)
      Failure(ex)
    }
  }

  def findByPrivateMetadata(privateMetadata: PrivateMetadata): Option[SlackTaskMeta] = {
    slackTaskMetaFactories.find(_.taskLogChannel.id == privateMetadata.value)
  }

}
