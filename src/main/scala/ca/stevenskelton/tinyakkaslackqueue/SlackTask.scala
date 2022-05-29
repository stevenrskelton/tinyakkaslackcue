package ca.stevenskelton.tinyakkaslackqueue

import ca.stevenskelton.tinyakkaslackqueue.timer.IdTask

trait SlackTask extends IdTask[SlackTaskThreadTs] {

  def ts: SlackTaskThreadTs

  override def id: SlackTaskThreadTs = ts

  def meta: SlackTaskMeta

  def createdBy: SlackUserId

  var estimatedCount: Int = 0

  var completedCount: Int = 0

  var isComplete: Boolean = false

  def percentComplete: Float = {
    if (isComplete) 1f
    else if (completedCount > 0 && estimatedCount > 0) math.min(0.99f, completedCount.toFloat / estimatedCount.toFloat)
    else 0f
  }

  def notifyOnError: Seq[SlackUserId]

  def notifyOnComplete: Seq[SlackUserId]
}