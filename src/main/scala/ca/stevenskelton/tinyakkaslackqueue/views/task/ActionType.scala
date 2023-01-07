package ca.stevenskelton.tinyakkaslackqueue.views.task

final case class ActionType(value: String) extends AnyVal
object ActionType {
  val multi_users_select: ActionType = ActionType("multi_users_select")
  val datepicker: ActionType = ActionType("datepicker")
  val timepicker: ActionType = ActionType("timepicker")
  val static_select: ActionType = ActionType("static_select")
  val plain_text_input: ActionType = ActionType("plain_text_input")
  val button: ActionType = ActionType("button")
  val channels_select: ActionType = ActionType("channels_select")
}
