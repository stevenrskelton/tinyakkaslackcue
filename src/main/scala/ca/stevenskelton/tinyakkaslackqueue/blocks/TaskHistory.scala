package ca.stevenskelton.tinyakkaslackqueue.blocks

import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils

import scala.collection.SortedSet

case class TaskHistory(
                        slackTaskIdentifier: SlackTaskIdentifier,
                        running: Option[ScheduledSlackTask],
                        executed: SortedSet[TaskHistoryItem],
                        pending: SortedSet[ScheduledSlackTask]
                      ) {

  def toBlocks: SlackBlocksAsString = {

    val executedBlocks = if (executed.isEmpty) "" else executed.toSeq.reverse.map(_.toBlocks.value).mkString(",", """,{"type": "divider"},""", "")

    val pendingBlocks = if (pending.isEmpty) "" else pending.map {
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


    val runningBlocks = running.map {
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

    val queueText = if (running.isEmpty) "Run Immediately" else "Queue Immediately"

    SlackBlocksAsString {
      s"""
{
  "type": "header",
  "text": {
    "type": "plain_text",
    "text": "${slackTaskIdentifier.name}",
    "emoji": true
  }
},{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "${slackTaskIdentifier.description}"
  }
},{
  "type": "actions",
  "elements": [
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "${queueText}"
      },
      "style": "primary",
      "action_id": "${ActionId.TaskQueue.value}",
      "value": "${slackTaskIdentifier.name}"
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
      "value": "${slackTaskIdentifier.name}"
    }
  ]
}
$runningBlocks
$pendingBlocks
$executedBlocks
"""
    }
  }

}
