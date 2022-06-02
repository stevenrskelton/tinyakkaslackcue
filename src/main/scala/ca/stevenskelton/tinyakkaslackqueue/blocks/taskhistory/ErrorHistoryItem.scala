package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.blocks.TaskFailure
import play.api.libs.json.Json

import java.time.ZonedDateTime

object ErrorHistoryItem {
  implicit val fmt = Json.format[ErrorHistoryItem]
  val Action = "error"
}

case class ErrorHistoryItem(ex: String, message: String, start: ZonedDateTime) extends TaskHistoryOutcomeItem {
  override def action: String = ErrorHistoryItem.Action

  override def sectionBlocks: Seq[String] = Seq(
    s"""{"type": "mrkdwn","text": "Error:\n$ex"}""",
    s"""{"type": "mrkdwn","text": "Message:\n$message"}"""
  )

  override def icon: String = TaskFailure
}
