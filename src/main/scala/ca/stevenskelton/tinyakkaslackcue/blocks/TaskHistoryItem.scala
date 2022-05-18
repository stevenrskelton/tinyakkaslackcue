package ca.stevenskelton.tinyakkaslackcue.blocks

import ca.stevenskelton.tinyakkaslackcue.blocks.HomeTab.State
import ca.stevenskelton.tinyakkaslackcue.util.DateUtils
import ca.stevenskelton.tinyakkaslackcue.{SlackBlocksAsString, SlackTs, SlackUserId}

import java.time.{Duration, ZonedDateTime}

object TaskHistoryItem {
  implicit val orderingFieldsInstance = new Ordering[TaskHistoryItem] {
    override def compare(x: TaskHistoryItem, y: TaskHistoryItem): Int = x.date.compareTo(y.date)
  }
}

case class TaskHistoryItem(slackTs: SlackTs, date: ZonedDateTime, duration: Duration, createdBy: SlackUserId, state: State.State) {

  def toBlocks: SlackBlocksAsString = {
    val blocksAsString = state match {
      case State.Success => s"""
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": ":white_check_mark: *Last Success:* ${DateUtils.humanReadable(date)}"
  },
  "accessory": {
    "type": "button",
    "text": {
      "type": "plain_text",
      "text": "View Logs",
      "emoji": true
    },
    "value": "click_me_123",
    "action_id": "button-action"
  }
}"""
      case State.Failure => s"""
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": ":no_entry_sign: *Last Failure:* ${DateUtils.humanReadable(date)}"
  },
  "accessory": {
    "type": "button",
    "text": {
      "type": "plain_text",
      "text": "View Logs",
      "emoji": true
    },
    "value": "click_me_123",
    "action_id": "button-action"
  }
}"""
      case State.Scheduled => s"""
{
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
    "value": "click_me_123",
    "action_id": "button-action"
  }
}"""
      case State.Running =>
        s"""
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
      "value": "click_me_123"
    },
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "Cancel"
      },
      "style": "danger",
      "value": "click_me_123"
    }
  ]
}"""
    }
    SlackBlocksAsString(blocksAsString)
  }
}

