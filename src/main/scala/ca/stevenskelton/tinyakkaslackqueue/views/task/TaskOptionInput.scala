package ca.stevenskelton.tinyakkaslackqueue.views.task

import ca.stevenskelton.tinyakkaslackqueue.SlackUser
import ca.stevenskelton.tinyakkaslackqueue.blocks.{ActionId, logLevelEmoji}
import org.slf4j.event.Level
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{JsObject, Json}

import java.time.ZonedDateTime

//https://api.slack.com/reference/block-kit/block-elements
abstract class TaskOptionInput(actionId: ActionId, label: String, text: String) {

  val actionType: ActionType
  val params: Seq[(String, JsValueWrapper)]

  def toJson: JsObject = Json.obj(
    "type" -> "input",
    "element" -> {
      Json.obj(
        "type" -> actionType.value,
        "placeholder" -> Json.obj(
          "type" -> "plain_text",
          "text" -> text,
          "emoji" -> true
        ),
        "action_id" -> actionId.value
      ) ++ Json.obj(params: _*)
    },
    "label" -> Json.obj(
      "type" -> "plain_text",
      "text" -> label,
      "emoji" -> true
    )
  )
}

object TaskOptionInput {

  def dataLogLevel(default: Level): TaskOptionInput = new TaskInputStaticSelect(
    ActionId.DataLogLevel,
    "Log Level",
    "Select an item",
    Seq(Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG).map(logLevelBlock),
    logLevelBlock(default)
  )

  private def logLevelBlock(level: Level): JsObject = Json.obj(
    "text" -> Json.obj(
      "type" -> "plain_text",
      "text" -> s"${logLevelEmoji(level)} ${level.name}",
      "emoji" -> true
    ),
    "value" -> level.name
  )

  def dataScheduleDate(default: ZonedDateTime): TaskOptionInput = new TaskInputDatepicker(
    ActionId.DataScheduleDate,
    label = "Start date",
    text = "Select a date",
    default
  )

  def dataScheduleTime(default: ZonedDateTime): TaskOptionInput = new TaskInputTimepicker(
    ActionId.DataScheduleTime,
    label = "Start Time",
    text = "Start time",
    default
  )

  def notifyOnComplete(slackUser: SlackUser): TaskOptionInput = new TaskInputMultiUserSelect(
    ActionId.DataNotifyOnComplete,
    label = "Users to notify on task complete",
    text = "Users",
    initialUsers = Seq(slackUser.id.value)
  )

  def notifyOnFailure(slackUser: SlackUser): TaskOptionInput = new TaskInputMultiUserSelect(
    ActionId.DataNotifyOnFailure,
    label = "Users to notify on task failure",
    text = "Users",
    initialUsers = Seq(slackUser.id.value)
  )

}