package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.SlackUserId
import play.api.libs.json.{Json, OFormat}

object CancelHistoryItem {
  implicit val fmt: OFormat[CancelHistoryItem] = Json.format[CancelHistoryItem]
  val Action = "cancel"
}

case class CancelHistoryItem(user: SlackUserId, currentCount: Int) extends TaskHistoryActionItem {
  override def action: String = CancelHistoryItem.Action
}

