package ca.stevenskelton.tinyakkaslackcue.blocks

import ca.stevenskelton.tinyakkaslackcue.blocks.HomeTab.{ActionIdTaskCancel, ActionIdTaskQueue, ActionIdTaskSchedule}
import ca.stevenskelton.tinyakkaslackcue.{InteractiveJavaUtilTimer, SlackBlocksAsString, SlackTask, SlackTaskIdentifier, SlackTs}

import scala.collection.SortedSet

case class TaskHistory(
                        slackTaskIdentifier: SlackTaskIdentifier,
                        running: Option[InteractiveJavaUtilTimer[SlackTask]#ScheduledTask],
                        executed: SortedSet[TaskHistoryItem],
                        pending: SortedSet[TaskHistoryItem]
                      ) {

  val nextTs: Option[SlackTs] = pending.headOption.map(_.slackTs)

  private val HeaderPreamble = "Scheduled Task "
  private val CreatedByPreamble = "*Created by* "
  private val ScheduledForPreamble = "*Scheduled for* "

  def toBlocks: SlackBlocksAsString = {

    val executedBlocks = if(executed.isEmpty)
      """,{
        			"type": "section",
        			"text": {
        				"type": "mrkdwn",
        				"text": "No previous executions"
        			}
        }""" else executed.toSeq.reverse.map(_.toBlocks.value).mkString(",",""",{"type": "divider"},""","")

    val pendingBlocks = if(pending.isEmpty)
      """,{
        			"type": "section",
        			"text": {
        				"type": "mrkdwn",
        				"text": "No pending executions"
        			}
        }""" else pending.map(_.toBlocks.value).mkString(",",""",{"type": "divider"},""","")

    val runningBlocks = running.map {
      scheduledTask =>
        s""",{
			"type": "section",
			"text": {
				"type": "mrkdwn",
				"text": "${SlackTaskThread.update(scheduledTask)}"
			},
			"accessory": {
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
