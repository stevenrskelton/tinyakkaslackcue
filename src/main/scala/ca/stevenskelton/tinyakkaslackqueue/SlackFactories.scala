package ca.stevenskelton.tinyakkaslackqueue

import akka.Done
import akka.stream.Materializer
import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackTaskFactories, SlackTaskFactory}
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.TaskHistory
import ca.stevenskelton.tinyakkaslackqueue.timer.InteractiveJavaUtilTimer
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import com.slack.api.methods.request.pins.PinsListRequest
import org.slf4j.Logger
import play.api.libs.json.{JsValue, Json}

import java.time.ZonedDateTime
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success, Try}

object SlackFactories {

  val ConfigurationThreadHeader = "Configuration\n"

  def initialize(slackTaskFactories: SlackTaskFactories)(implicit logger: Logger, slackClient: SlackClient, materializer: Materializer): SlackFactories = {
    val botUserId = slackClient.slackConfig.botUserId.value
    val pinnedMessages = Option(slackClient.client.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).channel(slackClient.slackConfig.botChannel.id)).getItems).map(_.asScala.filter(_.getCreatedBy == botUserId)).getOrElse(Nil)
    val pinnedConfig = pinnedMessages.find(_.getMessage.getText.startsWith(ConfigurationThreadHeader))
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

case class SlackTaskInitialized(slackTaskFactory: SlackTaskFactory[_, _], var slackTaskMeta: Option[SlackTaskMeta])

class SlackFactories private(val slackTasks: Seq[SlackTaskInitialized])(implicit val logger: Logger, val slackClient: SlackClient, materializer: Materializer) {

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
      notifyOnComplete = Nil,
      mainLogger = logger
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
    slackTasks.drop(taskIndex).headOption.fold(false) {
      slackTaskInitialized =>
        slackTaskInitialized.slackTaskMeta.map {
          _.updateTaskLogChannel(slackChannel)
        }.getOrElse {
          SlackQueueThread.create(slackTaskInitialized.slackTaskFactory) match {
            case Left(queueThread) =>
              slackTaskInitialized.slackTaskMeta = Some(SlackTaskMeta.skipHistory(taskIndex, slackClient, slackChannel, queueThread, slackTaskInitialized.slackTaskFactory))
            case Right(error) =>
              logger.error(s"Could not create slack history thread for ${slackTaskInitialized.slackTaskFactory.name.getText}: $error")
              return false
          }
        }
        slackClient.slackConfig.persistConfig(this)
    }
  }


}
