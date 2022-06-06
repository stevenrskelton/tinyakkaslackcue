package ca.stevenskelton.tinyakkaslackqueue.api

import akka.Done
import akka.stream.Materializer
import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.TaskHistory
import ca.stevenskelton.tinyakkaslackqueue.timer.InteractiveJavaUtilTimer
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import org.slf4j.Logger

import java.time.ZonedDateTime
import scala.util.{Failure, Success, Try}

abstract class SlackFactories()(implicit val logger: Logger, val slackClient: SlackClient, materializer: Materializer) {

  protected val factories: List[SlackTaskFactory[_, _]]

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

  def factoryLogChannels: Seq[(SlackTaskFactory[_, _], Option[TaskLogChannel])] = factories.map {
    factory => factory -> slackClient.slackConfig.taskChannels.find(_._1 == factory.name.getText).map(_._2._1)
  }

  def updateFactoryLogChannel(taskIndex: Int, slackChannel: SlackChannel): Boolean = {
    factories.drop(taskIndex).headOption.map {
      slackTaskFactory =>
        val result = slackClient.slackConfig.setFactoryLogChannel(slackTaskFactory, slackChannel)
        if (result) initializeFromConfig else false
    }.getOrElse(false)
  }

  private def initializeFromConfig: Boolean = {
    var hasError = false
    val meta = factories.zipWithIndex.flatMap {
      case (factory, index) =>
        slackClient.slackConfig.taskChannels.find(_._1 == factory.name.getText).map {
          case (_, (taskLogChannel, slackHistoryThread)) => SlackTaskMeta.initialize(index, slackClient, taskLogChannel, slackHistoryThread, factory)
        }.orElse {
          logger.error(s"slackTaskMetaFactories: No config set for task `${factory.name.getText}`")
          hasError = true
          None
        }
    }
    _slackTaskMetaFactories = if (hasError) Nil else meta
    !hasError
  }

  private var _slackTaskMetaFactories: Seq[SlackTaskMeta] = Nil

  def slackTaskMetaFactories: Seq[SlackTaskMeta] = {
    if (_slackTaskMetaFactories.isEmpty) initializeFromConfig
    _slackTaskMetaFactories
  }

  initializeFromConfig
}
