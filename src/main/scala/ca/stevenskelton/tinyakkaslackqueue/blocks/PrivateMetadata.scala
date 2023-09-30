package ca.stevenskelton.tinyakkaslackqueue.blocks

import play.api.libs.json.Json.JsValueWrapper

case class PrivateMetadata(value: String) extends AnyVal {
  def block: (String, JsValueWrapper) = "private_metadata" -> value
}

object PrivateMetadata {
  val Empty: PrivateMetadata = PrivateMetadata("")
}