package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.SlackUserId
import play.api.libs.json.Json

object CancelHistoryItem {
  implicit val fmt = Json.format[CancelHistoryItem]
}

case class CancelHistoryItem(user: SlackUserId) extends TaskHistoryActionItem

