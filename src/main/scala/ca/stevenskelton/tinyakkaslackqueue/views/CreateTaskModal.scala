package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.*
import ca.stevenskelton.tinyakkaslackqueue.blocks.*
import ca.stevenskelton.tinyakkaslackqueue.views.task.TaskOptionInput
import org.slf4j.event.Level
import play.api.libs.json.{JsObject, Json}

import java.time.ZonedDateTime

class CreateTaskModal(slackPayload: SlackPayload, slackTaskMeta: SlackTaskMeta, zonedDateTimeOpt: Option[ZonedDateTime])(implicit slackFactories: SlackFactories) extends SlackModal {

  override def toString: String = Json.stringify(blocks)

  private val submitButtonText = if (zonedDateTimeOpt.isEmpty) {
    if (slackFactories.isExecuting) "Queue" else "Run"
  } else {
    "Schedule"
  }

  private val dateTimeBlocks = zonedDateTimeOpt.map {
    zonedDateTime =>
      Seq(
        TaskOptionInput.dataScheduleDate(zonedDateTime).toJson,
        TaskOptionInput.dataScheduleTime(zonedDateTime).toJson
      )
  }.getOrElse(Nil)

  private def advancedOptions: Seq[JsObject] = {
    val taskOptions = slackTaskMeta.factory.taskOptions(slackPayload)
    if (taskOptions.isEmpty) {
      Nil
    } else {
      Json.obj("type" -> "divider") +: taskOptions.map(_.toJson)
    }
  }

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
        TaskOptionInput.dataLogLevel(Level.WARN).toJson
      )
    }
  )

}
