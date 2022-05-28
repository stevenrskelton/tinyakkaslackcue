package ca.stevenskelton.tinyakkaslackqueue

import akka.Done
import akka.actor.ActorSystem
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.SlackTaskThread
import ca.stevenskelton.tinyakkaslackqueue.lib.SlackTaskMeta
import ca.stevenskelton.tinyakkaslackqueue.timer.InteractiveJavaUtilTimer
import com.typesafe.config.Config
import org.slf4j.Logger

import java.time.ZonedDateTime
import scala.util.Try

class TinySlackQueue(slackClient: SlackClient, logger: Logger, onComplete: (SlackTask, Try[Done]) => Unit)(implicit actorSystem: ActorSystem, config: Config) {
  private val interactiveTimer = new InteractiveJavaUtilTimer[SlackTs, SlackTask](logger)

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

}
