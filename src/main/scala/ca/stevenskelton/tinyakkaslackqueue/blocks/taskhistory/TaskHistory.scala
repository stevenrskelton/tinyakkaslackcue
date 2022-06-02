package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId
import ca.stevenskelton.tinyakkaslackqueue.timer.TextProgressBar
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import ca.stevenskelton.tinyakkaslackqueue.views.HomeTab
import ca.stevenskelton.tinyakkaslackqueue.views.HomeTab.{cancelTaskButton, viewLogsButton}
import ca.stevenskelton.tinyakkaslackqueue.{ScheduledSlackTask, SlackTaskMeta}

import java.time.Duration
import scala.collection.SortedSet

case class TaskHistory(
                        slackTaskMeta: SlackTaskMeta,
                        running: Option[(ScheduledSlackTask, Option[TaskHistoryItem[CancelHistoryItem]])],
                        executed: SortedSet[TaskHistoryItem[TaskHistoryOutcomeItem]],
                        pending: SortedSet[ScheduledSlackTask]
                      ) {

  def homeTabBlocks: String = {
    val viewHistoryBlocks = if(executed.isEmpty) "" else
        s""",{
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "View History (${executed.size})"
      },
      "action_id": "${ActionId.TaskHistory.value}",
      "value": "${slackTaskMeta.taskChannel.value}"
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
      "action_id": "${ActionId.TaskQueue.value}",
      "value": "${slackTaskMeta.taskChannel.value}"
    },
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "Schedule"
      },
      "style": "primary",
      "action_id": "${ActionId.TaskSchedule.value}",
      "value": "${slackTaskMeta.taskChannel.value}"
    }
    $viewHistoryBlocks
  ]
}
${TaskHistory.homeTabRunningBlocks(running)}
${TaskHistory.homeTabPendingBlocks(pending)}
"""
  }

  def executionHistoryBlocks: Seq[String] = executed.toSeq.reverse.map(TaskHistory.taskHistoryOutcomeBlocks)
}

object TaskHistory {
  def homeTabRunningBlocks(running: Option[(ScheduledSlackTask, Option[TaskHistoryItem[CancelHistoryItem]])]): String = {
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
      "text": "*When:*Submitted Aut 10"
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

  def taskHistoryOutcomeBlocks(taskHistoryItem: TaskHistoryItem[TaskHistoryOutcomeItem]): String = {
    val duration = Duration.between(taskHistoryItem.time, taskHistoryItem.action.start)
    s"""
   {
			"type": "section",
			"text": {
				"type": "mrkdwn",
				"text": "*#team-updates*\n<fakelink.toUrl.com|Q4 Team Projects> posts project updates to <fakelink.toChannel.com|#team-updates>"
			},
			"accessory": ${HomeTab.viewLogsButton(taskHistoryItem.taskId)}
    },{
      "type": "section",
      "fields": [
        {
          "type": "mrkdwn",
          "text": "*Type:*\n${taskHistoryItem.action.action}"
        },
        {
          "type": "mrkdwn",
          "text": "*Duration:*\n${DateUtils.humanReadable(duration)}"
        },
        ${taskHistoryItem.action.sectionBlocks.mkString(",")}
      ]
    }"""
  }

  def homeTabPendingBlocks(pending: SortedSet[ScheduledSlackTask]): String = {
    if (pending.isEmpty) ""
    else pending.map {
      scheduledTask =>
        s"""{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": ":watch: *Scheduled:* ${DateUtils.humanReadable(scheduledTask.executionStart)}"
  },
  "accessory": {
    "type": "button",
    "text": {
      "type": "plain_text",
      "text": "View Details",
      "emoji": true
    },
    "value": "${scheduledTask.task.id.value}",
    "action_id": "${ActionId.TaskView}"
  }
}"""
    }.mkString(",", """,{"type": "divider"},""", "")
  }
}