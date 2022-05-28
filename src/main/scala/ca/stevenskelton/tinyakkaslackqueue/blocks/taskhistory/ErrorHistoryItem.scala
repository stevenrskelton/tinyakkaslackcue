package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import play.api.libs.json.Json

object ErrorHistoryItem {
  implicit val fmt = Json.format[ErrorHistoryItem]
}

case class ErrorHistoryItem(ex: String, message: String) extends TaskHistoryActionItem
