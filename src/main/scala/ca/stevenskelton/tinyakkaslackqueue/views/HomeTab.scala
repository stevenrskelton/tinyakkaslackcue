package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId
import ca.stevenskelton.tinyakkaslackqueue.{ScheduledSlackTask, SlackFactories, SlackThread}
import play.api.libs.json.{JsObject, Json}

import java.time.ZoneId

object HomeTab {

  def viewLogsButton(slackThread: SlackThread): JsObject = Json.obj(
    "type" -> "button",
    "text" -> Json.obj(
      "type" -> "plain_text",
      "text" -> "View Logs",
      "emoji" -> true
    ),
    "url" -> slackThread.url,
    "action_id" -> ActionId.RedirectToTaskThread.value
  )

  def cancelTaskButton(scheduledTask: ScheduledSlackTask, actionId: ActionId): JsObject = Json.obj(
    "type" -> "button",
    "text" -> Json.obj(
      "type" -> "plain_text",
      "text" -> "Cancel Task",
      "emoji" -> true
    ),
    "style" -> "danger",
    "value" -> scheduledTask.task.id.value,
    "action_id" -> actionId.value,
    "confirm" -> Json.obj(
      "title" -> Json.obj(
        "type" -> "plain_text",
        "text" -> s"Cancel task ${scheduledTask.task.meta.factory.name.getText}"
      ),
      "text" -> Json.obj(
        "type" -> "mrkdwn",
        "text" -> {
          if (scheduledTask.isRunning) "Task will be notified to abort execution as soon as possible."
          else "This task hasn't been started and will be removed from queue."
        }
      ),
      "confirm" -> Json.obj(
        "type" -> "plain_text",
        "text" -> "Cancel Task"
      ),
      "deny" -> Json.obj(
        "type" -> "plain_text",
        "text" -> "Do not Cancel"
      )
    )
  )

}

class HomeTab(zoneId: ZoneId)(implicit slackFactories: SlackFactories) extends SlackHomeTab {

  //TODO: sort taskHistories
  private val taskHistories = slackFactories.history

  override def toString: String = Json.stringify(blocks)

  def blocks: JsObject = {
    if (taskHistories.isEmpty || taskHistories.size != slackFactories.slackTasks.size) new HomeTabConfigure(zoneId).blocks else
      Json.obj(
        "type" -> "home",
        "blocks" -> {

          Seq(
            Json.obj(
              "type" -> "actions",
              "elements" -> Seq(
                Json.obj(
                  "type" -> "button",
                  "text" -> Json.obj(
                    "type" -> "plain_text",
                    "text" -> ":arrows_counterclockwise: Refresh Statuses",
                    "emoji" -> true
                  ),
                  "style" -> "primary",
                  "action_id" -> ActionId.HomeTabRefresh.value
                ), Json.obj(
                  "type" -> "button",
                  "text" -> Json.obj(
                    "type" -> "plain_text",
                    "text" -> ":wrench: Configure",
                    "emoji" -> true
                  ),
                  "action_id" -> ActionId.HomeTabConfiguration.value
                )
              )
            ),
            Json.obj(
              "type" -> "divider"
            )
          ) ++ taskHistories.flatMap { obj =>
            val tabBlocks = obj.homeTabBlocks(zoneId)
            if (tabBlocks.isEmpty) {
              Nil
            } else {
              tabBlocks :+ Json.obj("type" -> "divider")
            }
          }
        }
      )
  }
}