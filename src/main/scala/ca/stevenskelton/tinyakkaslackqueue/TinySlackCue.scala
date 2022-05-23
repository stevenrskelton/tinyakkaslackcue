package ca.stevenskelton.tinyakkaslackqueue

import akka.Done
import akka.actor.ActorSystem
import ca.stevenskelton.tinyakkaslackqueue.blocks.SlackTaskThread
import com.typesafe.config.Config
import org.slf4j.Logger

import java.time.ZonedDateTime
import java.util.UUID
import scala.util.Try

class TinySlackCue(slackClient: SlackClient, logger: Logger, onComplete: (SlackTask, Try[Done]) => Unit)(implicit actorSystem: ActorSystem, config: Config) {
  private val interactiveTimer = new InteractiveJavaUtilTimer[SlackTask](logger)

  def listScheduledTasks: Seq[InteractiveJavaUtilTimer[SlackTask]#ScheduledTask] = interactiveTimer.list

  def cancelScheduledTask(uuid: UUID): Option[InteractiveJavaUtilTimer[SlackTask]#ScheduledTask] = interactiveTimer.cancel(uuid)

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
    slackClient.chatUpdateBlocks(SlackTaskThread.schedule(scheduledTask), slackTask.ts)
    slackTask
  }

}
