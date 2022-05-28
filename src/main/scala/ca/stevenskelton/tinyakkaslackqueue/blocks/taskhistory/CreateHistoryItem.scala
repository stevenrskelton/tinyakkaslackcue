package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.SlackUserId
import play.api.libs.json.Json

object CreateHistoryItem {
  implicit val fmt = Json.format[CreateHistoryItem]
}

case class CreateHistoryItem(user: SlackUserId) extends TaskHistoryActionItem
