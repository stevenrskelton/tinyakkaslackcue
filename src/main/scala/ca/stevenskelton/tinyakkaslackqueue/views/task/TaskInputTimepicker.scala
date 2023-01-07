package ca.stevenskelton.tinyakkaslackqueue.views.task

import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId
import play.api.libs.json.Json.JsValueWrapper

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TaskInputTimepicker(actionId: ActionId, label: String, text: String, default: ZonedDateTime)
  extends TaskOptionInput(actionId, label, text) {

  override val actionType: ActionType = ActionType.timepicker
  override val params: Seq[(String, JsValueWrapper)] = Seq("initial_time" -> default.format(DateTimeFormatter.ofPattern(" hh: mm")))
}
