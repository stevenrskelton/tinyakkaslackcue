package ca.stevenskelton.tinyakkaslackqueue.blocks

import play.api.libs.json.Json.JsValueWrapper

case class CallbackId(value: String) extends AnyVal {
  def block: (String, JsValueWrapper) = "callback_id" -> value
}

object CallbackId {
  val Create = CallbackId("task-create")
  val HomeTabConfigure = CallbackId("hometab-configure")
}
