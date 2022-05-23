package ca.stevenskelton.tinyakkaslackqueue.blocks

import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import ca.stevenskelton.tinyakkaslackqueue.{SlackBlocksAsString, SlackTs, SlackUserId}
import play.api.libs.json.Json

import java.time.{Duration, ZonedDateTime}

object TaskHistoryItem {
  implicit val ordering = new Ordering[TaskHistoryItem] {
    override def compare(x: TaskHistoryItem, y: TaskHistoryItem): Int = x.date.compareTo(y.date)
  }
  implicit val fmt = Json.format[TaskHistoryItem]
}

case class TaskHistoryItem(slackTs: SlackTs, date: ZonedDateTime, duration: Duration, createdBy: SlackUserId, isSuccess: Boolean) {

  def toBlocks: SlackBlocksAsString = {
    val blocksAsString = if (isSuccess) {
      s"""
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
    } else {
      s"""
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
    }
    SlackBlocksAsString(blocksAsString)
  }
}

