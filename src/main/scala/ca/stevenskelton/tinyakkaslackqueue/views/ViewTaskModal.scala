package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import ca.stevenskelton.tinyakkaslackqueue.{AppModalTitle, ScheduledSlackTask}
import play.api.libs.json.{JsObject, Json}

import java.time.ZoneId

class ViewTaskModal(zoneId: ZoneId, scheduledTasks: Seq[ScheduledSlackTask], index: Int) extends SlackModal {

  override def toString: String = Json.stringify(blocks)

  def blocks: JsObject = {
    val scheduledTask = scheduledTasks(index)
    val bodyBlocks = if (scheduledTask.isRunning) {
      Json.obj(
        "type" -> "section",
        "text" -> Json.obj(
          "type" -> "plain_text",
          "text" -> s"*Started:* ${DateUtils.humanReadable(scheduledTask.executionStart.withZoneSameInstant(zoneId))}"
        )
      )
    } else {
      val isQueueExecuting = scheduledTasks.head.isRunning
      Json.obj(
        "type" -> "section",
        "text" -> Json.obj(
          "type" -> "mrkdwn",
          "text" -> s"*Scheduled for:* ${DateUtils.humanReadable(scheduledTask.executionStart.withZoneSameInstant(zoneId))}\n*Queue Position*: ${if (index == 0 || (isQueueExecuting && index == 1)) "Next" else (index + 1).toString}"
        )
      )
    }

    Json.obj(
      "title" -> Json.obj(
        "type" -> "plain_text",
        "text" -> AppModalTitle,
        "emoji" -> true
      ),
      "type" -> "modal",
      "close" -> Json.obj(
        "type" -> "plain_text",
        "text" -> "Close",
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
          ),
          Json.obj(
            "type" -> "context",
            "elements" -> Seq(Json.obj(
              "type" -> "mrkdwn",
              "text" -> scheduledTask.task.meta.factory.description.getText
            )
            )
          ), Json.obj(
            "type" -> "actions",
            "elements" -> Seq(
              HomeTab.cancelTaskButton(scheduledTask, ActionId.TaskCancel)
            )
          )
        ) :+ bodyBlocks
      }
    )
  }
}