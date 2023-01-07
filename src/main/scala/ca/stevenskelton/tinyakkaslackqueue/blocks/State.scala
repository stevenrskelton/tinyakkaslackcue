package ca.stevenskelton.tinyakkaslackqueue.blocks

import ca.stevenskelton.tinyakkaslackqueue.views.task.ActionType
import ca.stevenskelton.tinyakkaslackqueue.{SlackChannel, SlackUserId}
import play.api.libs.json._

import java.time.{LocalDate, LocalTime}

trait State {}

case class DatePickerState(value: LocalDate) extends State

case class TimePickerState(value: LocalTime) extends State

case class MultiUsersState(users: Seq[SlackUserId]) extends State

case class SelectState(value: String) extends State

case class TextState(value: String) extends State

case class ButtonState(value: String) extends State

case class ChannelsState(value: SlackChannel) extends State

object State {

  implicit val rd: Reads[State] = new Reads[State] {
    override def reads(json: JsValue): JsResult[State] = {
      val state = (json \ "type").asOpt[String].map(ActionType(_)) match {
        case Some(ActionType.datepicker) => DatePickerState((json \ "selected_date").as[LocalDate])
        case Some(ActionType.timepicker) => TimePickerState((json \ "selected_time").as[LocalTime])
        case Some(ActionType.multi_users_select) => MultiUsersState((json \ "selected_users").as[Seq[String]].map(SlackUserId(_)))
        case Some(ActionType.static_select) => SelectState((json \ "selected_option" \ "value").as[String])
        case Some(ActionType.plain_text_input) => TextState((json \ "value").as[String])
        case Some(ActionType.button) => ButtonState((json \ "value").asOpt[String].getOrElse(""))
        case Some(ActionType.channels_select) => ChannelsState(new SlackChannel {
          override def id: String = (json \ "selected_channel").as[String]
        })
        case Some(x) => return JsError(s"Unknown type `$x`")
        case None => return JsError(s"No type")
      }
      JsSuccess(state)
    }
  }

  def parseActionStates(jsLookupResult: JsLookupResult): Map[ActionId, State] = {
    val actionStates = jsLookupResult.as[JsObject].values.flatMap {
      _.as[JsObject].value.map {
        case (action, value) => (ActionId(action), value.as[State])
      }
    }
    actionStates.toMap
  }

}