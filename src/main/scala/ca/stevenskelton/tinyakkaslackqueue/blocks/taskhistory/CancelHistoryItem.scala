package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.SlackUserId
import play.api.libs.json.Json

import java.time.ZonedDateTime

object CancelHistoryItem {
  implicit val fmt = Json.format[CancelHistoryItem]
  val Action = "cancel"
}

case class CancelHistoryItem(user: SlackUserId, start: ZonedDateTime) extends TaskHistoryOutcomeItem {
  override def action: String = CancelHistoryItem.Action

  override def sectionBlocks: Seq[String] = Seq(s"""{"type": "mrkdwn","text": "Cancelled by:\n${user.value}"}""")
}

