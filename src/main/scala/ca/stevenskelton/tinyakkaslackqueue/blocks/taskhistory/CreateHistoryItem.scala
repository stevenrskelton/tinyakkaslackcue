package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.SlackUserId
import play.api.libs.json.{Json, OFormat}

object CreateHistoryItem {
  implicit val fmt: OFormat[CreateHistoryItem] = Json.format[CreateHistoryItem]
  val Action = "create"
}

case class CreateHistoryItem(user: SlackUserId) extends TaskHistoryActionItem {
  override def action: String = CreateHistoryItem.Action
}
