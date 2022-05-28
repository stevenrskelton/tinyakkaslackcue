package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import play.api.libs.json.Json

object RunHistoryItem {
  implicit val fmt = Json.format[RunHistoryItem]
}

case class RunHistoryItem() extends TaskHistoryActionItem