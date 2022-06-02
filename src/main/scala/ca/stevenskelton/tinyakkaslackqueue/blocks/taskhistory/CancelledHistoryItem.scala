package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.SlackUserId
import ca.stevenskelton.tinyakkaslackqueue.blocks.TaskCancelled
import play.api.libs.json.Json

import java.time.ZonedDateTime

object CancelledHistoryItem {
  implicit val fmt = Json.format[ErrorHistoryItem]
  val Action = "cancelled"
}

case class CancelledHistoryItem(user: SlackUserId, start: ZonedDateTime) extends TaskHistoryOutcomeItem {
  override def action: String = CancelledHistoryItem.Action

  override def sectionBlocks: Seq[String] = Seq(
    s"""{"type": "mrkdwn","text": "Cancelled By:\n${user.value}"}"""
  )

  override def icon: String = TaskCancelled
}
