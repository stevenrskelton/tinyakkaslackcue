package ca.stevenskelton.tinyakkaslackcue.blocks

import ca.stevenskelton.tinyakkaslackcue.SlackUserId
import play.api.libs.json.{JsLookupResult, JsObject}

import java.time.{LocalDate, LocalTime}

trait State {}

case class DatePickerState(value: LocalDate) extends State

case class TimePickerState(value: LocalTime) extends State

case class MultiUsersState(users: Seq[SlackUserId]) extends State

case class SelectState(value: String) extends State

object State {

  def parseActionStates(jsLookupResult: JsLookupResult): Map[ActionId, State] = {
    val actionStates = jsLookupResult.as[JsObject].values.flatMap {
      _.as[JsObject].value.map {
        case (action, value) => (ActionId(action), State.parse(value.as[JsObject]))
      }
    }
    actionStates.toMap
  }

  def parse(jsObject: JsObject): State = {
    (jsObject \ "type").as[String] match {
      case "datepicker" => DatePickerState((jsObject \ "selected_date").as[LocalDate])
      case "timepicker" => TimePickerState((jsObject \ "selected_time").as[LocalTime])
      case "multi_users_select" => MultiUsersState((jsObject \ "selected_users").as[Seq[String]].map(SlackUserId(_)))
      case "static_select" => SelectState((jsObject \ "selected_option" \ "value").as[String])
    }
  }
}