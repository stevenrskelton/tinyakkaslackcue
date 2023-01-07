package ca.stevenskelton.tinyakkaslackqueue.views.task

import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId
import play.api.libs.json.Json.JsValueWrapper

class TaskInputMultiUserSelect(actionId: ActionId, label: String, text: String, initialUsers: Seq[String])
  extends TaskOptionInput(actionId, label, text) {

  override val actionType: ActionType = ActionType.multi_users_select
  override val params: Seq[(String, JsValueWrapper)] = Seq("initial_users" -> initialUsers)
}
