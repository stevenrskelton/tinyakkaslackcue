package ca.stevenskelton.tinyakkaslackcue.blocks

import ca.stevenskelton.tinyakkaslackcue.blocks.HomeTab.{ActionIdTaskCancel, ActionIdTaskQueue, ActionIdTaskSchedule, ActionIdTaskThread}
import ca.stevenskelton.tinyakkaslackcue._

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

    val executedBlocks = if (executed.isEmpty)
      """,{
        			"type": "section",
        			"text": {
        				"type": "mrkdwn",
        				"text": "No previous executions"
        			}
        }""" else executed.toSeq.reverse.map(_.toBlocks.value).mkString(",", """,{"type": "divider"},""", "")

    val pendingBlocks = if (pending.isEmpty)
      """,{
        			"type": "section",
        			"text": {
        				"type": "mrkdwn",
        				"text": "No pending executions"
        			}
        }""" else pending.map {
      scheduledTask =>
        s"""{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": ":watch: *Scheduled:* 2022-05-01 4:51pm"
  },
  "accessory": {
    "type": "button",
    "text": {
      "type": "plain_text",
      "text": "View Details",
      "emoji": true
    },
    "value": "${scheduledTask.uuid.toString}",
    "action_id": "${HomeTab.ActionIdTaskView}"
  }
}"""
    }.mkString(",", """,{"type": "divider"},""", "")


    val runningBlocks = running.map {
      scheduledTask =>
        s"""{
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
      "action_id": "${ActionIdTaskThread.value}",
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
      "action_id": "${ActionIdTaskCancel.value}",
      "value": "${scheduledTask.uuid}"
    }
  ]
}"""
    }.getOrElse("")

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
        "text": "Queue"
      },
      "style": "primary",
      "action_id": "${ActionIdTaskQueue.value}",
      "value": "${slackTaskIdentifier.name}"
    },
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "Schedule"
      },
      "action_id": "${ActionIdTaskSchedule.value}",
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
