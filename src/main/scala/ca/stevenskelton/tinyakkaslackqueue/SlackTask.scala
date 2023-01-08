package ca.stevenskelton.tinyakkaslackqueue

import ca.stevenskelton.tinyakkaslackqueue.timer.IdTask
import org.slf4j.event.Level

import java.time.ZonedDateTime

trait SlackTask extends IdTask[SlackTs] {

  def slackTaskThread: SlackTaskThread

  override def id: SlackTs = slackTaskThread.ts

  def meta: SlackTaskMeta

  def logLevel: Level

  def createdBy: SlackUserId

  var estimatedCount: Int = 0

  var completedCount: Int = 0

  var isComplete: Boolean = false

  var runStart: Option[ZonedDateTime] = None

  def percentComplete: Float = {
    if (isComplete) 1f
    else if (completedCount > 0 && estimatedCount > 0) math.min(0.99f, completedCount.toFloat / estimatedCount.toFloat)
    else 0f
  }

}