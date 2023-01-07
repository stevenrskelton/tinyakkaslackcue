package ca.stevenskelton.tinyakkaslackqueue.views.task

import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId
import play.api.libs.json.Json.JsValueWrapper

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TaskInputDatepicker(actionId: ActionId, label: String, text: String, default: ZonedDateTime)
  extends TaskOptionInput(actionId, label, text) {

  override val actionType: ActionType = ActionType.datepicker
  override val params: Seq[(String, JsValueWrapper)] = Seq("initial_date" -> default.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
}
