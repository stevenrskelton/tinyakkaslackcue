package ca.stevenskelton.tinyakkaslackqueue.views.task

import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId
import play.api.libs.json.Json.JsValueWrapper

class TaskInputPlainText(actionId: ActionId, label: String, text: String, initial: String, minLength: Int = 0, maxLength: Int = 3000)
  extends TaskOptionInput(actionId, label, text) {

  override val actionType: ActionType = ActionType.plain_text_input
  override val params: Seq[(String, JsValueWrapper)] = Seq("initial_value" -> initial, "min_length" -> minLength, "max_length" -> maxLength)
}