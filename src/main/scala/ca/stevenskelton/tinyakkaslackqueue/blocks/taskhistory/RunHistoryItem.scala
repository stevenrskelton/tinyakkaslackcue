package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import play.api.libs.json.{Json, OFormat}

object RunHistoryItem {
  implicit val fmt: OFormat[RunHistoryItem] = Json.format[RunHistoryItem]
  val Action = "run"
}

case class RunHistoryItem(estimatedCount: Int) extends TaskHistoryActionItem {
  override def action: String = RunHistoryItem.Action
}