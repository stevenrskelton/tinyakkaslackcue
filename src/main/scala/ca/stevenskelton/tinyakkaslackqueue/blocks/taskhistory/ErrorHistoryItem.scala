package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.blocks.TaskFailure
import play.api.libs.json.{JsObject, Json}

import java.time.ZonedDateTime

object ErrorHistoryItem {
  implicit val fmt = Json.format[ErrorHistoryItem]
  val Action = "error"
}

case class ErrorHistoryItem(ex: String, message: String, start: ZonedDateTime) extends TaskHistoryOutcomeItem {
  override def action: String = ErrorHistoryItem.Action

  override def sectionBlocks: Seq[JsObject] = Seq(
    Json.obj("type" -> "mrkdwn", "text" -> s"Error:\n$ex"),
    Json.obj("type" -> "mrkdwn", "text" -> s"Message:\n$message")
  )

  override def icon: String = TaskFailure
}
