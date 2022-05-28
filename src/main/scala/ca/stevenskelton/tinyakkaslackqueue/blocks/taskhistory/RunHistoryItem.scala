package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import play.api.libs.json.Json

import java.time.ZonedDateTime

object RunHistoryItem {
  implicit val fmt = Json.format[RunHistoryItem]
  val Action = "run"
}

case class RunHistoryItem(start: ZonedDateTime) extends TaskHistoryActionItem {
  override def action: String = RunHistoryItem.Action
}