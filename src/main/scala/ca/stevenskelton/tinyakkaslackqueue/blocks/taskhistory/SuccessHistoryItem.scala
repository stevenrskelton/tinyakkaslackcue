package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import play.api.libs.json.Json

object SuccessHistoryItem {
  implicit val fmt = Json.format[SuccessHistoryItem]
}

case class SuccessHistoryItem(count: Int) extends TaskHistoryActionItem
