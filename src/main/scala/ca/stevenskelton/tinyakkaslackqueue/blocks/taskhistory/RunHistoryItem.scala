package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import play.api.libs.json.Json

object RunHistoryItem {
  implicit val fmt = Json.format[RunHistoryItem]
  val Action = "run"
}

case class RunHistoryItem(estimatedCount: Int) extends TaskHistoryActionItem {
  override def action: String = RunHistoryItem.Action
}