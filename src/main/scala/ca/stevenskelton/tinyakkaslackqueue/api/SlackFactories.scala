package ca.stevenskelton.tinyakkaslackqueue.api

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{Materializer, SystemMaterializer}
import ca.stevenskelton.tinyakkaslackqueue
import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.SlackTaskThread
import ca.stevenskelton.tinyakkaslackqueue.blocks.{PrivateMetadata, TaskHistory}
import ca.stevenskelton.tinyakkaslackqueue.timer.InteractiveJavaUtilTimer
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.conversations.{ConversationsCreateRequest, ConversationsListRequest}
import com.slack.api.methods.request.pins.{PinsAddRequest, PinsListRequest}
import com.slack.api.model.{ConversationType, Message}
import com.typesafe.config.Config
import org.slf4j.Logger

import java.time.ZonedDateTime
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import scala.util.{Failure, Success, Try}

abstract class SlackFactories(
                               val slackClient: SlackClient,
                               val logger: Logger,
                               val actorSystem: ActorSystem,
                               val config: Config
                             ) {

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
    val slackPlaceholder = slackClient.chatPostMessage(SlackTaskThread.placeholderThread(slackTaskMeta.factory))
    implicit val sc = slackClient
    val slackTask = slackTaskMeta.factory.create(
      slackTaskMeta,
      ts = SlackTs(slackPlaceholder),
      createdBy = SlackUserId.Empty,
      notifyOnError = Nil,
      notifyOnComplete = Nil
    )
    val scheduledTask = time.fold(interactiveTimer.schedule(slackTask, onComplete(slackTask, _)))(interactiveTimer.schedule(slackTask, _, onComplete(slackTask, _)))
    //    slackClient.chatUpdateBlocks(SlackTaskThread.schedule(scheduledTask), slackTask.ts)
    scheduledTask
  }





  protected val factories: Seq[SlackTaskFactory[_, _]]

  private def parsePinnedMessage(message: Message): Seq[SlackTaskMeta] = {
    //    val conversationsResult = slackClient.client.conversationsList((r: ConversationsListRequest.ConversationsListRequestBuilder) => r.token(slackClient.botOAuthToken).types(Seq(ConversationType.PUBLIC_CHANNEL).asJava))
    //    val channels = conversationsResult.getChannels.asScala
    //    val results = message.getText.lines.toList.asScala.flatMap {
    //      line =>
    //        val channelName = line.takeWhile(_ != ' ')
    //        val name = line.drop(channelName.length)
    //        for {
    //          channel <- channels.find(_.getName == channelName)
    //          factory <- factories.find(_.name.getText == name)
    //        }yield {
    //          val pinnedMessages = Option(slackClient.client.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(slackClient.botOAuthToken).channel(channel.getId)).getItems).map(_.asScala).getOrElse(Nil)
    //          val pinnedResult = pinnedMessages.filter {
    //            o => o.getCreatedBy == slackClient.botUserId.value
    //          }
    //          val historyThread = pinnedResult.headOption.map(o => SlackTs(o.getMessage)).getOrElse {
    //            val pinnedMessageResult = slackClient.client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackClient.botOAuthToken).channel(channel.getId).text("History"))
    //            slackClient.client.pinsAdd((r: PinsAddRequest.PinsAddRequestBuilder) => r.token(slackClient.botOAuthToken).channel(channel.getId).timestamp(pinnedMessageResult.getTs))
    //            SlackTs(pinnedMessageResult.getMessage)
    //          }
    //          SlackTaskMeta(SlackChannel(channel),historyThread,factory )
    //        }
    //    }
    //    results.toSeq
    Nil
  }

  def history: Seq[TaskHistory] = {
    val allTasks = listScheduledTasks
    slackTaskMetaFactories.map(_.history(allTasks))
  }

  lazy val slackTaskMetaFactories: Seq[SlackTaskMeta] = {

    //    val pinnedMessages = Option(slackClient.client.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(slackClient.botOAuthToken).channel(slackClient.botChannel.value)).getItems).map(_.asScala).getOrElse(Nil)
    //    val pinnedResult = pinnedMessages.filter {
    //      o => o.getCreatedBy == slackClient.botUserId.value && o.getMessage.getText != "Task History"
    //    }
    //    pinnedResult.headOption.map { o =>
    //      //TODO: parse history
    //      parsePinnedMessage(o.getMessage)
    //    }.getOrElse {
    //      val body = ""
    //      val pinnedMessageResult = slackClient.client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackClient.botOAuthToken).channel(slackClient.botChannel.value).text(body))
    //      slackClient.client.pinsAdd((r: PinsAddRequest.PinsAddRequestBuilder) => r.token(slackClient.botOAuthToken).channel(slackClient.botChannel.value).timestamp(pinnedMessageResult.getTs))
    //      parsePinnedMessage(pinnedMessageResult.getMessage)
    //    }

    val conversationsResult = slackClient.client.conversationsList((r: ConversationsListRequest.ConversationsListRequestBuilder) => r.token(slackClient.botOAuthToken).types(Seq(ConversationType.PUBLIC_CHANNEL).asJava))
    val channels = conversationsResult.getChannels.asScala
    factories.map {
      factory =>
        val name = factory.name.getText.filter(_.isLetterOrDigit).toLowerCase
        val channel = channels.find(_.getName == name).getOrElse {
          val createdChannel = slackClient.client.conversationsCreate((r: ConversationsCreateRequest.ConversationsCreateRequestBuilder) => r.token(slackClient.botOAuthToken).name(name).isPrivate(false))
          if (!createdChannel.isOk) logger.error(createdChannel.getError)
          createdChannel.getChannel
        }
        val pinnedMessages = Option(slackClient.client.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(slackClient.botOAuthToken).channel(channel.getId)).getItems).map(_.asScala).getOrElse(Nil)
        val pinnedResult = pinnedMessages.filter {
          o => o.getCreatedBy == slackClient.botUserId.value
        }
        val historyThread = pinnedResult.headOption.map(o => SlackTs(o.getMessage)).getOrElse {
          val pinnedMessageResult = slackClient.client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackClient.botOAuthToken).channel(channel.getId).text("History"))
          if (!pinnedMessageResult.isOk) logger.error(pinnedMessageResult.getError)
          val pinsAddedResult = slackClient.client.pinsAdd((r: PinsAddRequest.PinsAddRequestBuilder) => r.token(slackClient.botOAuthToken).channel(channel.getId).timestamp(pinnedMessageResult.getTs))
          if (!pinsAddedResult.isOk) logger.error(pinsAddedResult.getError)
          SlackTs(pinnedMessageResult.getMessage)
        }
        tinyakkaslackqueue.SlackTaskMeta(SlackChannel(channel), historyThread, factory)
    }
  }

  def findByPrivateMetadata(privateMetadata: PrivateMetadata): Option[SlackTaskMeta] = {
    slackTaskMetaFactories.find(_.channel.value == privateMetadata.value)
  }

}
