package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.{AppModalTitle, ScheduledSlackTask}
import play.api.libs.json.{JsObject, Json}

class CancelTaskModal(scheduledTask: ScheduledSlackTask) extends SlackModal {

  override def toString: String = Json.stringify(blocks)

  def blocks: JsObject = Json.obj(
    "type" -> "modal",
    "close" -> Json.obj(
      "type" -> "plain_text",
      "text" -> "Close",
      "emoji" -> true
    ),
    "clear_on_close" -> true,
    "title" -> Json.obj(
      "type" -> "plain_text",
      "text" -> AppModalTitle,
      "emoji" -> true
    ),
    "blocks" -> {
      Seq(
        Json.obj(
          "type" -> "header",
          "text" -> Json.obj(
            "type" -> "plain_text",
            "text" -> scheduledTask.task.meta.factory.name.getText,
            "emoji" -> true
          )
        ), Json.obj(
          "type" -> "context",
          "elements" -> Seq(Json.obj(
            "type" -> "mrkdwn",
            "text" -> scheduledTask.task.meta.factory.description.getText
          ))
        ),
      ) :+ {

        if (scheduledTask.isRunning) {
          Json.obj(
            "type" -> "section",
            "text" -> Json.obj(
              "type" -> "mrkdwn",
              "text" -> "Task has already started, attempting to cancel."
            ),
            "accessory" -> HomeTab.viewLogsButton(scheduledTask.task.slackTaskThread)
          )
        } else {
          Json.obj(
            "type" -> "section",
            "text" -> Json.obj(
              "type" -> "plain_text",
              "text" -> "Removed task from queue.",
              "emoji" -> true
            )
          )
        }

      }
    }
  )

}
