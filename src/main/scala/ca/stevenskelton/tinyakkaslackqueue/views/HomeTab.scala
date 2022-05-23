package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.ScheduledSlackTask
import ca.stevenskelton.tinyakkaslackqueue.blocks.{ActionId, TaskHistory}
import ca.stevenskelton.tinyakkaslackqueue.timer.TextProgressBar
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import ca.stevenskelton.tinyakkaslackqueue.views.HomeTab.{cancelTaskButton, viewLogsButton}

object HomeTab {

  def viewLogsButton(scheduledTask: ScheduledSlackTask) = s"""
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "View Logs"
      },
      "action_id": "${ActionId.TaskThread}",
      "value": "${scheduledTask.task.id.value}"
    }"""

  def cancelTaskButton(scheduledTask: ScheduledSlackTask) = s"""
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "text": "Cancel Task",
        "emoji": true
      },
      "style": "danger",
      "value": "${scheduledTask.task.id.value}",
      "action_id": "${ActionId.TaskCancel}",
      "confirm": {
        "title": {
            "type": "plain_text",
            "text": "Cancel task ${scheduledTask.task.meta.factory.name.getText}"
        },
        "text": {
            "type": "mrkdwn",
            "text": "${if (scheduledTask.isRunning) "Task will be notified to abort execution as soon as possible." else "This task hasn't been started and will be removed from queue."}"
        },
        "confirm": {
            "type": "plain_text",
            "text": "Cancel Task"
        },
        "deny": {
            "type": "plain_text",
            "text": "Do not Cancel"
        }
      }
    }"""
}

class HomeTab(taskHistories: Iterable[TaskHistory]) extends SlackView {

  private def taskHistoryBlocks(taskHistory: TaskHistory): String = {

    val executedBlocks = if (taskHistory.executed.isEmpty) "" else taskHistory.executed.toSeq.reverse.map(_.toBlocks.value).mkString(",", """,{"type": "divider"},""", "")

    val pendingBlocks = if (taskHistory.pending.isEmpty) "" else taskHistory.pending.map {
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


    val runningBlocks = taskHistory.running.map {
      scheduledTask =>
        s""",
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "${TextProgressBar.SlackEmoji.bar(scheduledTask.task.percentComplete, 40)}"
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
    ${viewLogsButton(scheduledTask)},
    ${cancelTaskButton(scheduledTask)}
  ]
},{"type": "divider"}"""
    }.getOrElse("")

    val queueText = if (taskHistory.running.isEmpty) "Run Immediately" else "Queue Immediately"

    s"""
{
  "type": "header",
  "text": {
    "type": "plain_text",
    "text": "${taskHistory.slackTaskMeta.factory.name.getText}",
    "emoji": true
  }
},{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "${taskHistory.slackTaskMeta.factory.description.getText}"
  }
},{
  "type": "actions",
  "elements": [
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "$queueText"
      },
      "style": "primary",
      "action_id": "${ActionId.TaskQueue.value}",
      "value": "${taskHistory.slackTaskMeta.channel.value}"
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
      "value": "${taskHistory.slackTaskMeta.channel.value}"
    }
  ]
}
$runningBlocks
$pendingBlocks
$executedBlocks
"""
  }

  private val blocks = if (taskHistories.isEmpty) {
    s"""
      {
        "type": "header",
        "text": {
          "type": "plain_text",
          "text": ":card_index: Tiny Akka Slack Cue",
          "emoji": true
        }
      },{
        "type": "section",
        "fields": [
          {
            "type": "mrkdwn",
            "text": "*Configure Tasks*\nTasks are cancellable and can be queued"
          }
        ]
      }"""
  } else {
    val header =
      s"""
      {
        "type": "actions",
        "elements": [
          {
            "type": "button",
            "text": {
              "type": "plain_text",
              "text": "Refresh :card_index: Statuses",
              "emoji": true
            },
            "style": "primary",
            "action_id": "${ActionId.TabRefresh}"
          }
        ]
      },{"type": "divider"},"""
    header + taskHistories.map(taskHistoryBlocks).mkString(""",{"type": "divider"},""")
  }

  override def toString: String = s"""{"type":"home","blocks":[$blocks]}"""
}