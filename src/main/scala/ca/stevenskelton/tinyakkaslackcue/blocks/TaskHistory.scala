package ca.stevenskelton.tinyakkaslackcue.blocks

import ca.stevenskelton.tinyakkaslackcue.blocks.HomeTab.{ActionIdTaskCancel, ActionIdTaskQueue, ActionIdTaskSchedule}
import ca.stevenskelton.tinyakkaslackcue.{SlackBlocksAsString, SlackTaskIdentifier, SlackTs}

import scala.collection.SortedSet

case class TaskHistory(
                        slackTaskIdentifier: SlackTaskIdentifier,
                        executed: SortedSet[TaskHistoryItem],
                        pending: SortedSet[TaskHistoryItem]
                      ) {

  val nextTs: Option[SlackTs] = pending.headOption.map(_.slackTs)

  private val HeaderPreamble = "Scheduled Task "
  private val CreatedByPreamble = "*Created by* "
  private val ScheduledForPreamble = "*Scheduled for* "

  def toBlocks: SlackBlocksAsString = {
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
  "type": "section",
  "fields": [
    {
      "type": "mrkdwn",
      "text": "*Type:*\nComputer (laptop)"
    },
    {
      "type": "mrkdwn",
      "text": "*When:*\nSubmitted Aut 10"
    },
    ${pending.map(_.toBlocks.value).mkString(""",{"type": "divider"},""")}
    ${executed.toSeq.reverse.map(_.toBlocks.value).mkString(""",{"type": "divider"},""")}
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
      "value": "${slackTaskIdentifier.name}"
    }
  ]
}"""
    }
  }

}
