package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.blocks.TaskSuccess
import play.api.libs.json.{JsObject, Json}

import java.time.ZonedDateTime

object SuccessHistoryItem {
  implicit val fmt = Json.format[SuccessHistoryItem]
  val Action = "success"
}

case class SuccessHistoryItem(count: Int, start: ZonedDateTime) extends TaskHistoryOutcomeItem {
  override def action: String = SuccessHistoryItem.Action

  override def sectionBlocks: Seq[JsObject] = Seq(Json.obj(
    "type" -> "mrkdwn", "text" -> s"*Final Item Count:* ${count.toString}"
  ))

  override def icon: String = TaskSuccess
}
