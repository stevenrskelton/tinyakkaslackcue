package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId
import ca.stevenskelton.tinyakkaslackqueue.{AppModalTitle, ScheduledSlackTask}

class CancelTaskModal(scheduledTask: ScheduledSlackTask) extends SlackView {

  private val blocks = if (scheduledTask.isRunning) {
    s"""
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "Task has already started, attempting to cancel."
  },
  "accessory": {
    "type": "button",
    "text": {
      "type": "plain_text",
      "text": "View Logs",
      "emoji": true
    },
    "value": "${scheduledTask.id.value}",
    "action_id": "${ActionId.TaskThread}"
  }
}"""
  } else {
    """
    {
			"type": "section",
			"text": {
				"type": "plain_text",
				"text": "Removed task from queue.",
				"emoji": true
			}
		}
    """
  }

  override def toString: String = {
    s"""
{
	"type": "modal",
	"close": {
		"type": "plain_text",
		"text": "Close",
		"emoji": true
	},
	"clear_on_close": true,
	"title": {
		"type": "plain_text",
		"text": "$AppModalTitle",
		"emoji": true
	},
	"blocks": [
 		{
			"type": "header",
			"text": {
				"type": "plain_text",
				"text": "${scheduledTask.task.name.getText}",
				"emoji": true
			}
		},{
			"type": "context",
			"elements": [
				{
					"type": "mrkdwn",
					"text": "${scheduledTask.task.description.getText}"
				}
			]
		},
		$blocks
	]
}"""
  }
}
