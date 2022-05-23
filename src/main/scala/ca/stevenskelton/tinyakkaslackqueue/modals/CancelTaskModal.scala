package ca.stevenskelton.tinyakkaslackqueue.modals

import ca.stevenskelton.tinyakkaslackqueue.blocks.{ActionId, SlackView}
import ca.stevenskelton.tinyakkaslackqueue.{AppModalTitle, InteractiveJavaUtilTimer, SlackTask, SlackTs}

class CancelTaskModal(scheduledTask: InteractiveJavaUtilTimer[SlackTs,SlackTask]#ScheduledTask) extends SlackView {
  override def name: String = "modal"

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
    "value": "${scheduledTask.id}",
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
				"text": "${scheduledTask.task.name}",
				"emoji": true
			}
		},{
			"type": "context",
			"elements": [
				{
					"type": "mrkdwn",
					"text": "${scheduledTask.task.description}"
				}
			]
		},
		$blocks
	]
}"""
  }
}
