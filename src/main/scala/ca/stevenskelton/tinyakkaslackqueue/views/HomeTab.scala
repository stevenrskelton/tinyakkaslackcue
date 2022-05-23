package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.TextProgressBar
import ca.stevenskelton.tinyakkaslackqueue.blocks.{ActionId, TaskHistory}
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils

class HomeTab(taskHistories: Seq[TaskHistory]) extends SlackView {

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
    "value": "${scheduledTask.id.toString}",
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
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "View Logs"
      },
      "action_id": "${ActionId.TaskThread.value}",
      "value": "${scheduledTask.id}"
    },
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "Cancel"
      },
      "style": "danger",
      "action_id": "${ActionId.TaskCancel.value}",
      "value": "${scheduledTask.id}"
    }
  ]
}"""
      }.getOrElse("")

      val queueText = if (taskHistory.running.isEmpty) "Run Immediately" else "Queue Immediately"

        s"""
{
  "type": "header",
  "text": {
    "type": "plain_text",
    "text": "${taskHistory.slackTaskIdentifier.name.getText}",
    "emoji": true
  }
},{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "${taskHistory.slackTaskIdentifier.description.getText}"
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
      "value": "${taskHistory.slackTaskIdentifier.name.getText}"
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
      "value": "${taskHistory.slackTaskIdentifier.name.getText}"
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