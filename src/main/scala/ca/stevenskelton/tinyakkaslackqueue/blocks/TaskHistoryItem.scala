package ca.stevenskelton.tinyakkaslackqueue.blocks

import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import ca.stevenskelton.tinyakkaslackqueue.{SlackBlocksAsString, SlackChannel, SlackTs, SlackUserId}
import play.api.libs.json.Json

import java.time.{Duration, ZonedDateTime}

object TaskHistoryItem {
  implicit val ordering = new Ordering[TaskHistoryItem] {
    override def compare(x: TaskHistoryItem, y: TaskHistoryItem): Int = x.date.compareTo(y.date)
  }
  implicit val fmt = Json.format[TaskHistoryItem]
}

case class TaskHistoryItem(ts: SlackTs, channel: SlackChannel, date: ZonedDateTime, duration: Duration, createdBy: SlackUserId, isSuccess: Boolean) {

  /*
    def cancel(slackTask: SlackTask, slackUserId: SlackUserId)(implicit slackFactories: SlackFactories): ChatPostMessageResponse = {
    val isRunning = slackFactories.tinySlackQueue.listScheduledTasks.find(_.id == slackTask.id).fold(false)(_.isRunning)
    val json = Json.obj("cancel" -> slackTask.id, "by" -> slackUserId, "running" -> isRunning)
    slackFactories.slackClient.chatPostMessageInThread(toMessage(json), slackFactories.slackClient.historyThread)
    }
   */

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

