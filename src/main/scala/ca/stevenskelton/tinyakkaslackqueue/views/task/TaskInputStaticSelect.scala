package ca.stevenskelton.tinyakkaslackqueue.views.task

import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId
import play.api.libs.json.JsObject
import play.api.libs.json.Json.JsValueWrapper

class TaskInputStaticSelect(actionId: ActionId, label: String, text: String, options: Seq[JsObject], initial: JsObject)
  extends TaskOptionInput(actionId, label, text) {

  override val actionType: ActionType = ActionType.static_select
  override val params: Seq[(String, JsValueWrapper)] = Seq("options" -> options, "initial_option" -> initial)
}
