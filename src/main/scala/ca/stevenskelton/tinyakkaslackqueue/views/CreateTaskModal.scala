package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.blocks._
import org.slf4j.event.Level
import play.api.libs.json.{JsObject, Json}

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CreateTaskModal(slackUser: SlackUser, slackTaskMeta: SlackTaskMeta, zonedDateTimeOpt: Option[ZonedDateTime])(implicit slackFactories: SlackFactories) extends SlackModal {

  private val submitButtonText = if (zonedDateTimeOpt.isEmpty) {
    if (slackFactories.isExecuting) "Queue" else "Run"
  } else {
    "Schedule"
  }

  private val dateTimeBlocks = zonedDateTimeOpt.map {
    zonedDateTime =>
      Seq(
        Json.obj(
          "type" -> "input",
          "element" -> Json.obj(
            "type" -> "datepicker",
            "initial_date" -> zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            "placeholder" -> Json.obj(
              "type" -> "plain_text",
              "text" -> "Select a date",
              "emoji" -> true
            ),
            "action_id" -> ActionId.DataScheduleDate.value
          ),
          "label" -> Json.obj(
            "type" -> "plain_text",
            "text" -> "Start date",
            "emoji" -> true
          )
        ),
        Json.obj(
          "type" -> "input",
          "element" -> Json.obj(
            "type" -> "timepicker",
            "initial_time" -> zonedDateTime.format(DateTimeFormatter.ofPattern(" hh: mm")),
            "placeholder" -> Json.obj(
              "type" -> "plain_text",
              "text" -> "Start Time",
              "emoji" -> true
            ),
            "action_id" -> ActionId.DataScheduleTime.value
          ),
          "label" -> Json.obj(
            "type" -> "plain_text",
            "text" -> "Start time",
            "emoji" -> true
          )
        )
      )
  }.getOrElse(Nil)

  private val advancedOptions = Seq(
    Json.obj("type" -> "divider"),
    Json.obj(
      "type" -> "input",
      "element" -> Json.obj(
        "type" -> "multi_users_select",
        "placeholder" -> Json.obj(
          "type" -> "plain_text",
          "text" -> "Users",
          "emoji" -> true
        ),
        "initial_users" -> Seq(slackUser.id.value),
        "action_id" -> ActionId.DataNotifyOnComplete.value
      ),
      "label" -> Json.obj(
        "type" -> "plain_text",
        "text" -> "Users to notify on task complete",
        "emoji" -> true
      )
    ),
    Json.obj(
      "type" -> "input",
      "element" -> Json.obj(
        "type" -> "multi_users_select",
        "placeholder" -> Json.obj(
          "type" -> "plain_text",
          "text" -> "Users",
          "emoji" -> true
        ),
        "initial_users" -> Seq(slackUser.id.value),
        "action_id" -> ActionId.DataNotifyOnFailure.value
      ),
      "label" -> Json.obj(
        "type" -> "plain_text",
        "text" -> "Users to notify on task failure",
        "emoji" -> true
      )
    )
  )

  private def logLevelBlock(level: Level): JsObject = Json.obj(
    "text" -> Json.obj(
      "type" -> "plain_text",
      "text" -> s"${logLevelEmoji(level)} ${level.name}",
      "emoji" -> true
    ),
    "value" -> level.name
  )

  def blocks: JsObject = Json.obj(
    PrivateMetadata(slackTaskMeta.index.toString).block,
    "title" -> Json.obj(
      "type" -> "plain_text",
      "text" -> AppModalTitle,
      "emoji" -> true
    ),
    "submit" -> Json.obj(
      "type" -> "plain_text",
      "text" -> submitButtonText,
      "emoji" -> true
    ),
    "type" -> "modal",
    CallbackId.Create.block,
    "close" -> Json.obj(
      "type" -> "plain_text",
      "text" -> "Cancel",
      "emoji" -> true
    ),
    "blocks" -> {
      Seq(
        Json.obj(
          "type" -> "header",
          "text" -> Json.obj(
            "type" -> "plain_text",
            "text" -> slackTaskMeta.factory.name.getText,
            "emoji" -> true
          )
        ), Json.obj(
          "type" -> "context",
          "elements" -> Seq(
            Json.obj(
              "type" -> "mrkdwn",
              "text" -> slackTaskMeta.factory.description.getText
            )
          )
        )) ++ dateTimeBlocks ++ advancedOptions ++ Seq(
        Json.obj("type" -> "divider"),
        Json.obj(
          "type" -> "input",
          "element" -> Json.obj(
            "type" -> "static_select",
            "placeholder" -> Json.obj(
              "type" -> "plain_text",
              "text" -> "Select an item",
              "emoji" -> true
            ),
            "options" -> Seq(Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG).map(logLevelBlock),
            "initial_option" -> logLevelBlock(Level.WARN),
            "action_id" -> ActionId.DataLogLevel.value
          ),
          "label" -> Json.obj(
            "type" -> "plain_text",
            "text" -> "Log Level",
            "emoji" -> true
          )
        )
      )
    }
  )

}
