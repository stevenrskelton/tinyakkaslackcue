package ca.stevenskelton.tinyakkaslackqueue.api

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{Materializer, SystemMaterializer}
import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.blocks.PrivateMetadata
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.TaskHistory
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.conversations.{ConversationsCreateRequest, ConversationsListRequest}
import com.slack.api.methods.request.pins.{PinsAddRequest, PinsListRequest}
import com.slack.api.model.ConversationType
import com.typesafe.config.Config
import org.slf4j.Logger

import java.time.ZonedDateTime
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import scala.util.{Failure, Success, Try}

abstract class SlackFactories()(implicit val logger: Logger, val slackClient: SlackClient, materializer: Materializer) {

  protected val factories: Seq[SlackTaskFactory[_, _]]

  private val interactiveTimer = SlackTaskMeta.createTimer(slackClient)

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
    val slackPlaceholder = slackClient.chatPostMessage(message, slackTaskMeta.taskChannel)
    val slackTask = slackTaskMeta.factory.create(
      slackTaskMeta,
      taskThread = SlackTaskThread(slackPlaceholder),
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

  lazy val slackTaskMetaFactories: Seq[SlackTaskMeta] = {

    val conversationsResult = slackClient.client.conversationsList((r: ConversationsListRequest.ConversationsListRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).types(Seq(ConversationType.PUBLIC_CHANNEL).asJava))
    val channels = conversationsResult.getChannels.asScala
    factories.map {
      factory =>
        val name = factory.name.getText.filter(_.isLetterOrDigit).toLowerCase
        val channel = channels.find(_.getName == name).getOrElse {
          logger.debug(s"Channel `$name` not found, creating.")
          val createdChannel = slackClient.client.conversationsCreate((r: ConversationsCreateRequest.ConversationsCreateRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).name(name).isPrivate(false))
          if (!createdChannel.isOk) {
            val ex = new Exception(s"Could not create channel $name: ${createdChannel.getError}")
            logger.error("slackTaskMetaFactories", ex)
            throw ex
          } else {
            createdChannel.getChannel
          }
        }
        val slackChannel = SlackChannel(channel)
        val pinnedMessages = Option(slackClient.client.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).channel(channel.getId)).getItems).map(_.asScala).getOrElse(Nil)
        val pinnedResult = pinnedMessages.filter {
          o => o.getCreatedBy == slackClient.slackConfig.botUserId.value
        }
        val historyThread = pinnedResult.headOption.map(o => SlackHistoryThread(o.getMessage, slackChannel)).getOrElse {
          val pinnedMessageResult = slackClient.client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).channel(channel.getId).text("History"))
          if (!pinnedMessageResult.isOk) logger.error(pinnedMessageResult.getError)
          val pinsAddedResult = slackClient.client.pinsAdd((r: PinsAddRequest.PinsAddRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).channel(channel.getId).timestamp(pinnedMessageResult.getTs))
          if (!pinsAddedResult.isOk) logger.error(pinsAddedResult.getError)
          SlackHistoryThread(pinnedMessageResult.getMessage, slackChannel)
        }
        SlackTaskMeta.initialize(slackClient, slackChannel, historyThread, factory)
    }
  }

  def findByChannel(slackChannel: SlackChannel): Try[SlackTaskMeta] = {
    slackTaskMetaFactories.find(_.taskChannel == slackChannel).map(Success(_)).getOrElse {
      val ex = new Exception(s"Task ${slackChannel.value} not found")
      logger.error("handleAction", ex)
      Failure(ex)
    }
  }

  def findByPrivateMetadata(privateMetadata: PrivateMetadata): Option[SlackTaskMeta] = {
    slackTaskMetaFactories.find(_.taskChannel.value == privateMetadata.value)
  }

}
