package ca.stevenskelton.tinyakkaslackcue.blocks

case class CallbackId(value: String) extends AnyVal {
  override def toString: String = value

  def block: String = s""""callback_id": "$value""""
}

object CallbackId {
  val View = CallbackId("task-view-modal")
  val Create = CallbackId("task-create")
}
