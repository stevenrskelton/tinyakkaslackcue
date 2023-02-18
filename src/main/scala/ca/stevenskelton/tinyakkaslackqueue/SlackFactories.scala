package ca.stevenskelton.tinyakkaslackqueue

import akka.Done
import akka.stream.Materializer
import ca.stevenskelton.tinyakkaslackqueue.SlackPayload.SlackPayloadType
import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackTaskFactories, SlackTaskFactory}
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.TaskHistory
import ca.stevenskelton.tinyakkaslackqueue.blocks.{ActionId, DatePickerState, SelectState, TimePickerState}
import ca.stevenskelton.tinyakkaslackqueue.logging.SlackResponseException
import ca.stevenskelton.tinyakkaslackqueue.timer.InteractiveJavaUtilTimer
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import com.slack.api.methods.request.pins.PinsListRequest
import com.typesafe.config.Config
import org.slf4j.Logger
import org.slf4j.event.Level
import play.api.libs.json.{JsValue, Json}

import java.time.{ZoneId, ZonedDateTime}
import java.util.TimerTask
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success, Try}

object SlackFactories {

  val ConfigurationThreadHeader = "Configuration\n"

  def initialize(slackTaskFactories: SlackTaskFactories, config: Config)(implicit logger: Logger, slackClient: SlackClient, materializer: Materializer): SlackFactories = {
    val botUserId = slackClient.slackConfig.botUserId.value
    val pinnedMessages = Option(slackClient.slackConfig.clientOption.get.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).channel(slackClient.slackConfig.botChannel.id)).getItems).map(_.asScala.filter(_.getCreatedBy == botUserId)).getOrElse(Nil)
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
    new SlackFactories(slackTasksInitialized, config)
  }
}

case class SlackTaskInitialized(slackTaskFactory: SlackTaskFactory[_, _], var slackTaskMeta: Option[SlackTaskMeta])

class SlackFactories private(val slackTasks: Seq[SlackTaskInitialized], config: Config)(implicit val logger: Logger, val slackClient: SlackClient, materializer: Materializer) {

  private val interactiveTimer = new InteractiveJavaUtilTimer[SlackTs, SlackTask]()

  private def queuePollingForScheduled(zonedDateTime: ZonedDateTime): Unit = {
    interactiveTimer.scheduleSystemTask(new TimerTask {
      override def run(): Unit = {
        queuePollingForScheduled(zonedDateTime.plusMinutes(10))

        val alreadyScheduled = listScheduledTasks
        slackTasks.foreach {
          slackTaskInitialized =>
            slackTaskInitialized.slackTaskFactory.nextRunDate(config).foreach {
              nextScheduledRun =>
                if (!alreadyScheduled.exists(task => slackTaskInitialized.slackTaskMeta.contains(task.task.meta) && task.executionStart == nextScheduledRun)) {
                  val actionStates = Map(
                    ActionId.DataScheduleDate -> DatePickerState(nextScheduledRun.toLocalDate),
                    ActionId.DataScheduleTime -> TimePickerState(nextScheduledRun.toLocalTime)
                  )
                  val slackPayload = SlackPayload(
                    payloadType = SlackPayload.BlockActions, viewId = "", user = SlackUser.System, actions = Nil, triggerId = SlackTriggerId.Empty,
                    privateMetadata = None, callbackId = None, actionStates

                  )
                  scheduleSlackTask(slackPayload, ZoneId.systemDefault())
                }
            }
        }
      }
    }, zonedDateTime)
  }
  queuePollingForScheduled(ZonedDateTime.now)

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

  def scheduleSlackTask(slackPayload: SlackPayload, zoneId: ZoneId): ScheduledSlackTask = {
    val zoneId = slackClient.userZonedId(slackPayload.user.id)
    val slackTaskMeta = slackTasks.drop(slackPayload.privateMetadata.map(_.value).flatMap(_.toIntOption).getOrElse(0)).head.slackTaskMeta.get
    val time = for {
      scheduledDate <- slackPayload.actionStates.get(ActionId.DataScheduleDate).map(_.asInstanceOf[DatePickerState].value)
      scheduledTime <- slackPayload.actionStates.get(ActionId.DataScheduleTime).map(_.asInstanceOf[TimePickerState].value)
    } yield scheduledDate.atTime(scheduledTime).atZone(zoneId)

    val message = time.map {
      zonedDateTime =>
        s"Scheduled task *${slackTaskMeta.factory.name.getText}* for ${DateUtils.humanReadable(zonedDateTime, zoneId)}"
    }.getOrElse {
      s"Queued task *${slackTaskMeta.factory.name.getText}*"
    }
    val slackPlaceholder = slackClient.chatPostMessage(message, slackTaskMeta.taskLogChannel)
    val slackTask = slackTaskMeta.factory.create(
      slackPayload,
      slackTaskMeta,
      taskThread = SlackTaskThread(slackPlaceholder, slackTaskMeta.taskLogChannel),
      createdBySlackUser = slackPayload.user.id,
      logLevel = slackPayload.actionStates.get(ActionId.DataLogLevel).map(o => Level.valueOf(o.asInstanceOf[SelectState].value)).getOrElse(Level.WARN),
      mainLogger = logger
    )
    val scheduledTask = time.fold(interactiveTimer.schedule(slackTask, onComplete(slackTask, _)))(interactiveTimer.schedule(slackTask, _, onComplete(slackTask, _)))
    SlackResponseException.logError(slackTaskMeta.historyAddCreate(scheduledTask), logger)
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
