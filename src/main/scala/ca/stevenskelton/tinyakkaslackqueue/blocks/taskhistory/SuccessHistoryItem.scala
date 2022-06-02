package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.blocks.TaskSuccess
import play.api.libs.json.Json

import java.time.ZonedDateTime

object SuccessHistoryItem {
  implicit val fmt = Json.format[SuccessHistoryItem]
  val Action = "success"
}

case class SuccessHistoryItem(count: Int, start: ZonedDateTime) extends TaskHistoryOutcomeItem {
  override def action: String = SuccessHistoryItem.Action

  override def sectionBlocks: Seq[String] = Seq(
    s"""{"type": "mrkdwn","text": "Item Count:\n${count.toString}"}"""
  )

  override def icon: String = TaskSuccess
}
