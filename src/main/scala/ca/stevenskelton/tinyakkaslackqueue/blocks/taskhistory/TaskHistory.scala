package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId
import ca.stevenskelton.tinyakkaslackqueue.timer.TextProgressBar
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import ca.stevenskelton.tinyakkaslackqueue.views.HomeTab
import ca.stevenskelton.tinyakkaslackqueue.views.HomeTab.{cancelTaskButton, viewLogsButton}
import ca.stevenskelton.tinyakkaslackqueue.{ScheduledSlackTask, SlackTaskMeta}
import play.api.libs.json.{JsObject, Json}

import java.time.{Duration, ZoneId}
import scala.collection.SortedSet

case class TaskHistory(
                        slackTaskMeta: SlackTaskMeta,
                        running: Option[(ScheduledSlackTask, Option[TaskHistoryItem[CancelHistoryItem]])],
                        executed: SortedSet[TaskHistoryItem[TaskHistoryOutcomeItem]],
                        pending: SortedSet[ScheduledSlackTask]
                      ) {

  def homeTabBlocks(zoneId: ZoneId): Seq[JsObject] = {
    val viewHistoryBlocks = {
      if (executed.isEmpty) Nil
      else Seq(Json.obj(
        "type" -> "button",
        "text" -> Json.obj(
          "type" -> "plain_text",
          "emoji" -> true,
          "text" -> s"View History (${executed.size})"
        ),
        "action_id" -> ActionId.HomeTabTaskHistory.value,
        "value" -> slackTaskMeta.index.toString
      ))
    }

    Seq(
      Json.obj(
        "type" -> "header",
        "text" -> Json.obj(
          "type" -> "plain_text",
          "text" -> slackTaskMeta.factory.name.getText,
          "emoji" -> true
        )
      ),
      Json.obj(
        "type" -> "section",
        "text" -> Json.obj(
          "type" -> "mrkdwn",
          "text" -> slackTaskMeta.factory.description.getText
        ),
      ),
      Json.obj(
        "type" -> "actions",
        "elements" -> {
          Seq(
            Json.obj(
              "type" -> "button",
              "text" -> Json.obj(
                "type" -> "plain_text",
                "emoji" -> true,
                "text" -> {
                  if (running.isEmpty) "Run Immediately" else "Queue"
                }
              ),
              "style" -> "primary",
              "action_id" -> ActionId.ModalTaskQueue.value,
              "value" -> slackTaskMeta.index.toString
            ),
            Json.obj(
              "type" -> "button",
              "text" -> Json.obj(
                "type" -> "plain_text",
                "emoji" -> true,
                "text" -> "Schedule"
              ),
              "style" -> "primary",
              "action_id" -> ActionId.ModalTaskSchedule.value,
              "value" -> slackTaskMeta.index.toString
            )
          ) ++ viewHistoryBlocks
        }
      )
    ) ++ TaskHistory.homeTabRunningBlocks(running, zoneId) ++ TaskHistory.homeTabPendingBlocks(pending, zoneId)
  }
}

object TaskHistory {
  def homeTabRunningBlocks(running: Option[(ScheduledSlackTask, Option[TaskHistoryItem[CancelHistoryItem]])], zoneId: ZoneId): Seq[JsObject] = {
    running.map {
      case (scheduledTask, cancellingOpt) =>
        val cancellingText = cancellingOpt.fold("")(historyTaskItem => "\nCancelling.")
        Seq(
          Json.obj(
            "type" -> "section",
            "text" -> Json.obj(
              "type" -> "mrkdwn",
              "text" -> s"${TextProgressBar.SlackEmoji.bar(scheduledTask.task.percentComplete, 40)}\n$cancellingText"
            )
          ),
          Json.obj(
            "type" -> "section",
            "fields" -> Seq(
              Json.obj(
                "type" -> "mrkdwn",
                "text" -> s"*Started:* ${DateUtils.humanReadable(scheduledTask.executionStart.withZoneSameInstant(zoneId))}"
              )
            ),
          ),
          Json.obj(
            "type" -> "actions",
            "elements" -> Seq(
              viewLogsButton(scheduledTask.task.slackTaskThread),
              cancelTaskButton(scheduledTask, ActionId.TaskCancel)
            )
          ),
          Json.obj("type" -> "divider")
        )
    }.getOrElse(Nil)
  }

  def taskHistoryOutcomeBlocks(taskHistoryItem: TaskHistoryItem[TaskHistoryOutcomeItem], zoneId: ZoneId): Seq[JsObject] = {
    val duration = Duration.between(taskHistoryItem.action.start, taskHistoryItem.time)

    val headerBlock = Json.obj(
      "type" -> "section",
      "text" -> Json.obj(
        "type" -> "mrkdwn",
        "text" -> s"${taskHistoryItem.action.icon} *${taskHistoryItem.action.action.toUpperCase}* ${DateUtils.humanReadable(taskHistoryItem.time.withZoneSameInstant(zoneId))}"
      ),
      "accessory" -> HomeTab.viewLogsButton(taskHistoryItem.taskId)
    )

    val durationBlock = Json.obj(
      "type" -> "mrkdwn",
      "text" -> s"*Duration:* ${DateUtils.humanReadable(duration)}"
    )

    val sectionBlocks = durationBlock +: taskHistoryItem.action.sectionBlocks

    Seq(
      headerBlock,
      Json.obj(
        "type" -> "section",
        "fields" -> sectionBlocks
      )
    )
  }

  def homeTabPendingBlocks(pending: SortedSet[ScheduledSlackTask], zoneId: ZoneId): Seq[JsObject] = {
    if (pending.isEmpty) Nil
    else {
      val pendingObjects = pending.toSeq.map {
        scheduledTask =>
          Json.obj(
            "type" -> "section",
            "text" -> Json.obj(
              "type" -> "mrkdwn",
              "text" -> s":watch: *Scheduled:* ${DateUtils.humanReadable(scheduledTask.executionStart.withZoneSameInstant(zoneId))}"
            ),
            "accessory" -> Json.obj(
              "type" -> "button",
              "text" -> Json.obj(
                "type" -> "plain_text",
                "text" -> "View Details",
                "emoji" -> true
              ),
              "value" -> scheduledTask.task.id.value,
              "action_id" -> ActionId.ModalQueuedTaskView.value
            )
          )
      }
      pendingObjects.flatMap(obj => Seq(obj, Json.obj("type" -> "divider"))).dropRight(1)
    }
  }
}