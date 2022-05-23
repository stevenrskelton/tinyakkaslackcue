package ca.stevenskelton.tinyakkaslackqueue

import akka.Done
import akka.actor.ActorSystem
import ca.stevenskelton.tinyakkaslackqueue.blocks.SlackTaskThread
import ca.stevenskelton.tinyakkaslackqueue.timer.InteractiveJavaUtilTimer
import com.typesafe.config.Config
import org.slf4j.Logger

import java.time.ZonedDateTime
import scala.util.Try

class TinySlackQueue(slackClient: SlackClient, logger: Logger, onComplete: (SlackTask, Try[Done]) => Unit)(implicit actorSystem: ActorSystem, config: Config) {
  private val interactiveTimer = new InteractiveJavaUtilTimer[SlackTs, SlackTask](logger)

  def listScheduledTasks: Seq[InteractiveJavaUtilTimer[SlackTs, SlackTask]#ScheduledTask] = interactiveTimer.list

  def cancelScheduledTask(slackTs: SlackTs): Option[InteractiveJavaUtilTimer[SlackTs, SlackTask]#ScheduledTask] = interactiveTimer.cancel(slackTs)

  def scheduleSlackTask(slackTaskFactory: SlackTaskFactory, time: Option[ZonedDateTime]): SlackTask = {
    val slackPlaceholder = slackClient.chatPostMessage(SlackTaskThread.placeholderThread(slackTaskFactory.name))
    implicit val sc = slackClient
    val slackTask = slackTaskFactory.create(
      ts = SlackTs(slackPlaceholder),
      createdBy = SlackUserId.Empty,
      notifyOnError = Nil,
      notifyOnComplete = Nil
    )
    val scheduledTask = time.fold(interactiveTimer.schedule(slackTask, onComplete(slackTask, _)))(interactiveTimer.schedule(slackTask, _, onComplete(slackTask, _)))
    slackClient.chatUpdateBlocks(SlackTaskThread.schedule(scheduledTask), slackTask.id)
    slackTask
  }

}
