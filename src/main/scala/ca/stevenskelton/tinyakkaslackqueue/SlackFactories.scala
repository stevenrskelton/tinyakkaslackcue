package ca.stevenskelton.tinyakkaslackqueue

import akka.Done
import akka.stream.Materializer
import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackTaskFactories, SlackTaskFactory}
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.{TaskHistory, TaskHistoryItem, TaskHistoryOutcomeItem}
import ca.stevenskelton.tinyakkaslackqueue.timer.InteractiveJavaUtilTimer
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.conversations.ConversationsListRequest
import com.slack.api.methods.request.pins.PinsListRequest
import com.slack.api.model.ConversationType
import org.slf4j.Logger
import play.api.libs.json.{JsValue, Json}

import java.time.ZonedDateTime
import scala.jdk.CollectionConverters.{CollectionHasAsScala, SeqHasAsJava}
import scala.util.{Failure, Success, Try}

object SlackFactories {

  val ConfigurationThreadHeader = "Configuration\n"
  val HistoryThreadHeader = "History"

//
//  private def initializeFromConfig: Boolean = {
//    var hasError = false
//    val meta = factories.zipWithIndex.flatMap {
//      case (factory, index) =>
//        slackClient.slackConfig.taskChannels.find(_._1 == factory.name.getText).map {
//          case (_, (taskLogChannel, slackHistoryThread)) => SlackTaskMeta.initialize(index, slackClient, taskLogChannel, slackHistoryThread, factory)
//        }.orElse {
//          logger.error(s"slackTaskMetaFactories: No config set for task `${factory.name.getText}`")
//          hasError = true
//          None
//        }
//    }
//    _slackTaskMetaFactories = if (hasError) Nil else meta
//    !hasError
//  }

  def initialize(slackTaskFactories: SlackTaskFactories)(implicit logger: Logger, slackClient: SlackClient, materializer: Materializer): SlackFactories = {
    val botUserId = slackClient.slackConfig.botUserId.value
    val pinnedMessages = Option(slackClient.client.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).channel(slackClient.slackConfig.botChannel.id)).getItems).map(_.asScala.filter(_.getCreatedBy == botUserId)).getOrElse(Nil)
    val pinnedConfig = pinnedMessages.find(_.getMessage.getText.startsWith(ConfigurationThreadHeader))

//    val conversationsResult = slackClient.client.conversationsList((r: ConversationsListRequest.ConversationsListRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).types(Seq(ConversationType.PUBLIC_CHANNEL).asJava))
    val slackTasksInitialized = pinnedConfig.map {
      messageItem =>
        val body = messageItem.getMessage.getText.drop(ConfigurationThreadHeader.length + 3).dropRight(3)
        val bodyJson = Json.parse(body)
        val taskChannelsJson = (bodyJson \ "taskchannels").asOpt[Seq[JsValue]].getOrElse(Nil)
        slackTaskFactories.factories.zipWithIndex.map {
          case (slackTaskFactory, index) =>
            val slackTaskMeta = taskChannelsJson.find(o => (o \ "task").asOpt[String].contains(slackTaskFactory.name.getText)).flatMap {
              taskJson =>
                val channelId = (taskJson \ "channelId").as[String]
                val taskLogChannel = TaskLogChannel(id = channelId)
                val queueTs = (taskJson \ "queueTs").as[String]
                val queueThread = SlackQueueThread(SlackTs(queueTs), slackClient.slackConfig.botChannel)
                SlackTaskMeta.readHistory(index, slackClient, taskLogChannel, queueThread, slackTaskFactory)
            }
            SlackTaskInitialized(slackTaskFactory, slackTaskMeta)
        }
    }.getOrElse {
      slackTaskFactories.factories.map(SlackTaskInitialized(_, None))
    }
    new SlackFactories(slackTasksInitialized)
  }
}

case class SlackTaskInitialized(slackTaskFactory: SlackTaskFactory[_,_], var slackTaskMeta: Option[SlackTaskMeta])

class SlackFactories private (val slackTasks: Seq[SlackTaskInitialized])(implicit val logger: Logger, val slackClient: SlackClient, materializer: Materializer) {

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
    slackTasks.flatMap(_.slackTaskMeta.map(_.history(allTasks)))
  }

  def factoryLogChannels: Seq[(SlackTaskFactory[_, _], Option[TaskLogChannel])] = slackTasks.map {
    slackTaskInitialized => slackTaskInitialized.slackTaskFactory -> slackTaskInitialized.slackTaskMeta.map(_.taskLogChannel)
  }

  def updateFactoryLogChannel(taskIndex: Int, slackChannel: TaskLogChannel): Boolean = {
    slackTasks.drop(taskIndex).headOption.map {
      slackTaskInitialized =>
        slackTaskInitialized.slackTaskMeta.map {
          _.updateTaskLogChannel(slackChannel)
        }.getOrElse {
          val message = s"History: ${slackTaskInitialized.slackTaskFactory.name.getText}"
          val post = slackClient.client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).channel(slackClient.slackConfig.botChannel.id).text(message))
          val queueThread = if (post.isOk) SlackQueueThread(SlackTs(post.getTs), slackClient.slackConfig.botChannel)
          else {
            logger.error(s"Could not create slack history thread for ${slackTaskInitialized.slackTaskFactory.name.getText}: ${post.getError}")
            return false
          }
          slackTaskInitialized.slackTaskMeta =  Some(SlackTaskMeta.skipHistory(taskIndex, slackClient, slackChannel, queueThread, slackTaskInitialized.slackTaskFactory))
        }


//            val conversationsResult = slackClient.client.conversationsList((r: ConversationsListRequest.ConversationsListRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).types(Seq(ConversationType.PUBLIC_CHANNEL).asJava))
//            val channels = Option(conversationsResult.getChannels.asScala).getOrElse(Nil)
//            val pinnedMessages = Option(slackClient.client.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).channel(slackClient.slackConfig.botChannel.id)).getItems).map(_.asScala.filter(_.getCreatedBy == slackClient.slackConfig.botUserId.value)).getOrElse(Nil)
//            channels.find(_.getId == slackChannel.id).map {
//              channel =>
//                val taskLogChannel = TaskLogChannel(id = channel.getId)
//                val message = s"Task: ${slackTaskInitialized.slackTaskFactory.name.getText}"
//                val slackHistoryThread = pinnedMessages.find(_.getMessage.getText.startsWith(message))
//                  .map(messageItem => SlackQueueThread(messageItem.getMessage, slackClient.slackConfig.botChannel))
//                  .getOrElse {
//                    val post = slackClient.client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).channel(slackClient.slackConfig.botChannel.id).text(message))
//                    if (post.isOk) SlackQueueThread(SlackTs(post.getTs), slackClient.slackConfig.botChannel)
//                    else {
//                      logger.error(s"Could not create slack history thread for ${slackTaskInitialized.slackTaskFactory.name.getText}: ${post.getError}")
//                      return false
//                    }
//                  }
//                taskChannels.update(slackTaskInitialized.slackTaskFactory.name.getText, (taskLogChannel, slackHistoryThread))
                slackClient.slackConfig.persistConfig(this)
//            }.getOrElse {
//              logger.error(s"Could not find channel `${slackChannel.id}` for ${slackTaskInitialized.slackTaskFactory.name.getText}")
//              return false
//            }
//
////        val result = slackClient.slackConfig.setFactoryLogChannel(, slackChannel)
//        if (result) initializeFromConfig else false
    }.getOrElse(false)
  }



}
