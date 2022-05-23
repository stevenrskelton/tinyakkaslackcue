package ca.stevenskelton.tinyakkaslackqueue

import akka.Done
import akka.actor.ActorSystem
import ca.stevenskelton.tinyakkaslackqueue.blocks.{PrivateMetadata, TaskHistory}
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.conversations.{ConversationsCreateRequest, ConversationsListRequest, ConversationsRepliesRequest}
import com.slack.api.methods.request.pins.{PinsAddRequest, PinsListRequest}
import com.slack.api.model.{ConversationType, Message}
import com.typesafe.config.Config
import org.slf4j.Logger

import scala.collection.SortedSet
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import scala.util.{Failure, Success, Try}

abstract class SlackFactories(
                               val slackClient: SlackClient,
                               val logger: Logger,
                               val actorSystem: ActorSystem,
                               val config: Config
                             ) {

  def onComplete(slackTask: SlackTask, result: Try[Done]): Unit = {
    result match {
      case Failure(ex) =>
      case Success(Done) =>
    }
  }

  val tinySlackQueue = new TinySlackQueue(slackClient, logger, onComplete)(actorSystem, config)

  //  private implicit val materializer = SystemMaterializer.get(actorSystem)
  protected val factories: Seq[SlackTaskFactory]

//  private def parsePinnedMessage(message: Message): Seq[SlackTaskMeta] = {
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
//  }

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
        val name = factory.name.getText.filter(_.isLetterOrDigit)
        val channel = channels.find(_.getName == name).getOrElse {
          val createdChannel = slackClient.client.conversationsCreate((r: ConversationsCreateRequest.ConversationsCreateRequestBuilder) => r.token(slackClient.botOAuthToken).name(name).isPrivate(false))
          createdChannel.getChannel
        }
        val pinnedMessages = Option(slackClient.client.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(slackClient.botOAuthToken).channel(channel.getId)).getItems).map(_.asScala).getOrElse(Nil)
        val pinnedResult = pinnedMessages.filter {
          o => o.getCreatedBy == slackClient.botUserId.value
        }
        val historyThread = pinnedResult.headOption.map(o => SlackTs(o.getMessage)).getOrElse {
          val pinnedMessageResult = slackClient.client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackClient.botOAuthToken).channel(channel.getId).text("History"))
          slackClient.client.pinsAdd((r: PinsAddRequest.PinsAddRequestBuilder) => r.token(slackClient.botOAuthToken).channel(channel.getId).timestamp(pinnedMessageResult.getTs))
          SlackTs(pinnedMessageResult.getMessage)
        }
        SlackTaskMeta(SlackChannel(channel),historyThread,factory )
    }
  }

  implicit val ordering = new Ordering[ScheduledSlackTask] {
    override def compare(x: ScheduledSlackTask, y: ScheduledSlackTask): Int = x.executionStart.compareTo(y.executionStart)
  }

  def history: Iterable[TaskHistory] = {
    val allQueuedTasks = tinySlackQueue.listScheduledTasks
    slackTaskMetaFactories.map {
      slackTaskMeta =>
        //TODO: read Slack thread

        var runningTask: Option[ScheduledSlackTask] = None
        val cueTasks = allQueuedTasks.withFilter(_.task.meta.channel == slackTaskMeta.channel).flatMap {
          scheduleTask =>
            if (scheduleTask.isRunning) {
              runningTask = Some(scheduleTask)
              None
            } else {
              Some(scheduleTask)
            }
        }
        TaskHistory(slackTaskMeta.factory, runningTask, executed = SortedSet.empty, pending = SortedSet.from(cueTasks))
    }
  }

  def findByPrivateMetadata(privateMetadata: PrivateMetadata): Option[SlackTaskMeta] = {
    slackTaskMetaFactories.find(_.channel.value == privateMetadata.value)
  }

}
