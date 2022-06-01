package ca.stevenskelton.tinyakkaslackqueue.api

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{Materializer, SystemMaterializer}
import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.blocks.PrivateMetadata
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.TaskHistory
import ca.stevenskelton.tinyakkaslackqueue.timer.InteractiveJavaUtilTimer
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

abstract class SlackFactories(
                               val slackClient: SlackClient,
                               val actorSystem: ActorSystem,
                               val config: Config
                             )(implicit logger: Logger) {

  protected val factories: Seq[SlackTaskFactory[_, _]]

  implicit val materializer: Materializer = SystemMaterializer.get(actorSystem).materializer

  private val interactiveTimer = new InteractiveJavaUtilTimer[SlackTs, SlackTask](logger)

  def onComplete(slackTask: SlackTask, result: Try[Done]): Unit = {
    result match {
      case Failure(ex) =>
      case Success(Done) =>
    }
  }

  def listScheduledTasks: Seq[ScheduledSlackTask] = interactiveTimer.list

  def isExecuting: Boolean = interactiveTimer.isExecuting

  def cancelScheduledTask(slackTs: SlackTs): Option[ScheduledSlackTask] = interactiveTimer.cancel(slackTs)

  def scheduleSlackTask(slackTaskMeta: SlackTaskMeta, time: Option[ZonedDateTime]): ScheduledSlackTask = {
    val message = time.map {
      zonedDateTime =>
        s"Scheduled task *${slackTaskMeta.factory.name.getText}* for ${DateUtils.humanReadable(zonedDateTime)}"
    }.getOrElse {
      s"Queued task *${slackTaskMeta.factory.name.getText}*"
    }
    val slackPlaceholder = slackClient.chatPostMessage(message, slackTaskMeta.taskChannel)
    implicit val sc = slackClient
    val slackTask = slackTaskMeta.factory.create(
      slackTaskMeta,
      taskThread = SlackTaskThread(slackPlaceholder),
      createdBy = SlackUserId.Empty,
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

    val conversationsResult = slackClient.client.conversationsList((r: ConversationsListRequest.ConversationsListRequestBuilder) => r.token(slackClient.botOAuthToken).types(Seq(ConversationType.PUBLIC_CHANNEL).asJava))
    val channels = conversationsResult.getChannels.asScala
    factories.map {
      factory =>
        val name = factory.name.getText.filter(_.isLetterOrDigit).toLowerCase
        val channel = channels.find(_.getName == name).getOrElse {
          logger.debug(s"Channel `$name` not found, creating.")
          val createdChannel = slackClient.client.conversationsCreate((r: ConversationsCreateRequest.ConversationsCreateRequestBuilder) => r.token(slackClient.botOAuthToken).name(name).isPrivate(false))
          if (!createdChannel.isOk){
            val ex = new Exception(s"Could not create channel $name: ${createdChannel.getError}")
            logger.error("slackTaskMetaFactories", ex)
            throw ex
          } else{
            createdChannel.getChannel
          }
        }
        val pinnedMessages = Option(slackClient.client.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(slackClient.botOAuthToken).channel(channel.getId)).getItems).map(_.asScala).getOrElse(Nil)
        val pinnedResult = pinnedMessages.filter {
          o => o.getCreatedBy == slackClient.botUserId.value
        }
        val historyThread = pinnedResult.headOption.map(o => SlackHistoryThread(o.getMessage)).getOrElse {
          val pinnedMessageResult = slackClient.client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackClient.botOAuthToken).channel(channel.getId).text("History"))
          if (!pinnedMessageResult.isOk) logger.error(pinnedMessageResult.getError)
          val pinsAddedResult = slackClient.client.pinsAdd((r: PinsAddRequest.PinsAddRequestBuilder) => r.token(slackClient.botOAuthToken).channel(channel.getId).timestamp(pinnedMessageResult.getTs))
          if (!pinsAddedResult.isOk) logger.error(pinsAddedResult.getError)
          SlackHistoryThread(pinnedMessageResult.getMessage)
        }
        SlackTaskMeta.initialize(slackClient, SlackChannel(channel), historyThread, factory)
    }
  }

  def findByPrivateMetadata(privateMetadata: PrivateMetadata): Option[SlackTaskMeta] = {
    slackTaskMetaFactories.find(_.taskChannel.value == privateMetadata.value)
  }

}
