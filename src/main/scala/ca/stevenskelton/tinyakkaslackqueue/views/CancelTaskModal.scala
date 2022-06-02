package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.{AppModalTitle, ScheduledSlackTask}

class CancelTaskModal(scheduledTask: ScheduledSlackTask) extends SlackModal {

  private val blocks = if (scheduledTask.isRunning) {
    s"""
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "Task has already started, attempting to cancel."
  },
  "accessory": ${HomeTab.viewLogsButton(scheduledTask.task.slackTaskThread)}
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
				"text": "${scheduledTask.task.meta.factory.name.getText}",
				"emoji": true
			}
		},{
			"type": "context",
			"elements": [
				{
					"type": "mrkdwn",
					"text": "${scheduledTask.task.meta.factory.description.getText}"
				}
			]
		},
		$blocks
	]
}"""
  }
}
