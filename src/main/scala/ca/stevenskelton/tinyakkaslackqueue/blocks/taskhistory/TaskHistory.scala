package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId
import ca.stevenskelton.tinyakkaslackqueue.timer.TextProgressBar
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import ca.stevenskelton.tinyakkaslackqueue.views.HomeTab
import ca.stevenskelton.tinyakkaslackqueue.views.HomeTab.{cancelTaskButton, viewLogsButton}
import ca.stevenskelton.tinyakkaslackqueue.{ScheduledSlackTask, SlackTaskMeta}

import java.time.{Duration, ZoneId}
import scala.collection.SortedSet

case class TaskHistory(
                        slackTaskMeta: SlackTaskMeta,
                        running: Option[(ScheduledSlackTask, Option[TaskHistoryItem[CancelHistoryItem]])],
                        executed: SortedSet[TaskHistoryItem[TaskHistoryOutcomeItem]],
                        pending: SortedSet[ScheduledSlackTask]
                      ) {

  def homeTabBlocks(zoneId: ZoneId): String = {
    val viewHistoryBlocks = if (executed.isEmpty) "" else
      s""",{
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "View History (${executed.size})"
      },
      "action_id": "${ActionId.HomeTabTaskHistory.value}",
      "value": "${slackTaskMeta.taskLogChannel.id}"
    }"""

    s"""
{
  "type": "header",
  "text": {
    "type": "plain_text",
    "text": "${slackTaskMeta.factory.name.getText}",
    "emoji": true
  }
},{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "${slackTaskMeta.factory.description.getText}"
  }
},{
  "type": "actions",
  "elements": [
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "${if (running.isEmpty) "Run Immediately" else "Queue"}"
      },
      "style": "primary",
      "action_id": "${ActionId.ModalTaskQueue.value}",
      "value": "${slackTaskMeta.taskLogChannel.id}"
    },
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "Schedule"
      },
      "style": "primary",
      "action_id": "${ActionId.ModalTaskSchedule.value}",
      "value": "${slackTaskMeta.taskLogChannel.id}"
    }
    $viewHistoryBlocks
  ]
}
${TaskHistory.homeTabRunningBlocks(running, zoneId)}
${TaskHistory.homeTabPendingBlocks(pending, zoneId)}
"""
  }

}

object TaskHistory {
  def homeTabRunningBlocks(running: Option[(ScheduledSlackTask, Option[TaskHistoryItem[CancelHistoryItem]])], zoneId: ZoneId): String = {
    running.map {
      case (scheduledTask, cancellingOpt) =>
        val cancellingText = cancellingOpt.fold("")(historyTaskItem => "\nCancelling.")
        s""",
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "${TextProgressBar.SlackEmoji.bar(scheduledTask.task.percentComplete, 40)}\n$cancellingText"
  }
},{
  "type": "section",
  "fields": [
    {
      "type": "mrkdwn",
      "text": "*Started:* ${DateUtils.humanReadable(scheduledTask.executionStart.withZoneSameInstant(zoneId))}"
    }
  ]
},{
  "type": "actions",
  "elements": [
    ${viewLogsButton(scheduledTask.task.slackTaskThread)},
    ${cancelTaskButton(scheduledTask, ActionId.TaskCancel)}
  ]
},{"type": "divider"}"""
    }.getOrElse("")
  }

  def taskHistoryOutcomeBlocks(taskHistoryItem: TaskHistoryItem[TaskHistoryOutcomeItem], zoneId: ZoneId): String = {
    val duration = Duration.between(taskHistoryItem.action.start, taskHistoryItem.time)
    s"""
   {
			"type": "section",
			"text": {
				"type": "mrkdwn",
				"text": "${taskHistoryItem.action.icon} *${taskHistoryItem.action.action.toUpperCase}* ${DateUtils.humanReadable(taskHistoryItem.time.withZoneSameInstant(zoneId))}"
			},
			"accessory": ${HomeTab.viewLogsButton(taskHistoryItem.taskId)}
    },{
      "type": "section",
      "fields": [
        {
          "type": "mrkdwn",
          "text": "*Duration:* ${DateUtils.humanReadable(duration)}"
        },
        ${taskHistoryItem.action.sectionBlocks.mkString(",")}
      ]
    }"""
  }

  def homeTabPendingBlocks(pending: SortedSet[ScheduledSlackTask], zoneId: ZoneId): String = {
    if (pending.isEmpty) ""
    else pending.map {
      scheduledTask =>
        s"""{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": ":watch: *Scheduled:* ${DateUtils.humanReadable(scheduledTask.executionStart.withZoneSameInstant(zoneId))}"
  },
  "accessory": {
    "type": "button",
    "text": {
      "type": "plain_text",
      "text": "View Details",
      "emoji": true
    },
    "value": "${scheduledTask.task.id.value}",
    "action_id": "${ActionId.ModalQueuedTaskView}"
  }
}"""
    }.mkString(",", """,{"type": "divider"},""", "")
  }
}