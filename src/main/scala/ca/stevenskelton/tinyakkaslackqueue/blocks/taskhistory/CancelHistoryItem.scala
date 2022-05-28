package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.SlackUserId
import play.api.libs.json.Json

object CancelHistoryItem {
  implicit val fmt = Json.format[CancelHistoryItem]
  val Action = "cancel"
}

case class CancelHistoryItem(user: SlackUserId, currentCount: Int) extends TaskHistoryActionItem {
  override def action: String = CancelHistoryItem.Action
}

