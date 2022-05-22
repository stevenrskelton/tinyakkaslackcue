package ca.stevenskelton.tinyakkaslackcue.blocks

import ca.stevenskelton.tinyakkaslackcue._
import ca.stevenskelton.tinyakkaslackcue.util.DateUtils

import scala.collection.SortedSet

case class TaskHistory(
                        slackTaskIdentifier: SlackTaskIdentifier,
                        running: Option[InteractiveJavaUtilTimer[SlackTask]#ScheduledTask],
                        executed: SortedSet[TaskHistoryItem],
                        pending: SortedSet[InteractiveJavaUtilTimer[SlackTask]#ScheduledTask]
                      ) {

  val nextTs: Option[SlackTs] = pending.headOption.map(_.task.ts)

  private val HeaderPreamble = "Scheduled Task "
  private val CreatedByPreamble = "*Created by* "
  private val ScheduledForPreamble = "*Scheduled for* "

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
    "value": "${scheduledTask.uuid.toString}",
    "action_id": "${ActionId.TaskView}"
  }
}"""
    }.mkString(",", """,{"type": "divider"},""", "")


    val runningBlocks = running.map {
      scheduledTask =>
        s""",{
  "type": "section",
  "fields": [
    {
      "type": "mrkdwn",
      "text": "${SlackTaskThread.update(scheduledTask)}"
    },
    {
      "type": "mrkdwn",
      "text": "*When:*\nSubmitted Aut 10"
    },
    {
      "type": "mrkdwn",
      "text": "*Last Update:*\nMar 10, 2015 (3 years, 5 months)"
    },
    {
      "type": "mrkdwn",
      "text": "*Reason:*\nAll vowel keys aren't working."
    },
    {
      "type": "mrkdwn",
      "text": "*Specs:*\n\"Cheetah Pro 15\" - Fast, really fast\""
    }
  ]
},
{
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
      "value": "${scheduledTask.uuid}"
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
      "value": "${scheduledTask.uuid}"
    }
  ]
}"""
    }.getOrElse("")

    val queueText = if(running.isEmpty) "Run Immediately" else "Queue Immediately"

    SlackBlocksAsString {
      s"""
{
  "type": "header",
  "text": {
    "type": "plain_text",
    "text": "${slackTaskIdentifier.name}",
    "emoji": true
  }
},
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "${slackTaskIdentifier.description}"
  }
},
{
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
