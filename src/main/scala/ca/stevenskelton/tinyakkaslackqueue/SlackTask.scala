package ca.stevenskelton.tinyakkaslackqueue

import ca.stevenskelton.tinyakkaslackqueue.lib.SlackTaskMeta
import ca.stevenskelton.tinyakkaslackqueue.timer.IdTask

abstract class SlackTask extends IdTask[SlackTs] {

  def ts: SlackTs

  override def id: SlackTs = ts

  def meta: SlackTaskMeta

  def createdBy: SlackUserId

  var estimatedCount: Int = 0

  var completedCount: Int = 0

  def percentComplete: Float =
    if (completedCount == 0) 0f
    else if (estimatedCount > 0) math.min(0.99f, completedCount.toFloat / estimatedCount.toFloat)
    else 0f

  def notifyOnError: Seq[SlackUserId]

  def notifyOnComplete: Seq[SlackUserId]
}