package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.blocks.{ActionId, CallbackId}
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import ca.stevenskelton.tinyakkaslackqueue.{AppModalTitle, ScheduledSlackTask}

class ViewTaskModal(scheduledTasks: Seq[ScheduledSlackTask], index: Int) extends SlackView {
  override def toString: String = {
    val scheduledTask = scheduledTasks(index)
    val bodyBlocks = if (scheduledTask.isRunning) {
      s""",{
          "type": "section",
          "text": {
            "type": "plain_text",
            "text": "*Started:* ${DateUtils.humanReadable(scheduledTask.executionStart)}"
          }
        }"""
    } else {
      val isQueueExecuting = scheduledTasks.head.isRunning
      s""",{
          "type": "section",
          "text": {
            "type": "mrkdwn",
            "text": "*Scheduled for:* ${scheduledTask.executionStart.toString}\n*Queue Position*: ${if (index == 0 || (isQueueExecuting && index == 1)) "Next" else (index + 1).toString}"
          }
        }"""
    }

    s"""{
      "title": {
        "type": "plain_text",
        "text": "$AppModalTitle",
        "emoji": true
      },
      "type": "modal",
      ${CallbackId.View.block},
      "close": {
        "type": "plain_text",
        "text": "Close",
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
        },{
          "type": "actions",
          "elements": [
            {
              "type": "button",
              "text": {
                "type": "plain_text",
                "text": "Cancel Task",
                "emoji": true
              },
              "style": "danger",
              "value": "${scheduledTask.id.toString}",
              "action_id": "${ActionId.TaskCancel}",
              "confirm": {
                "title": {
                    "type": "plain_text",
                    "text": "Cancel task ${scheduledTask.task.name.getText}"
                },
                "text": {
                    "type": "mrkdwn",
                    "text": "${if (scheduledTask.isRunning) "Task will be notified to abort execution as soon as possible." else "This task hasn't been started and will be removed from queue."}"
                },
                "confirm": {
                    "type": "plain_text",
                    "text": "Cancel Task"
                },
                "deny": {
                    "type": "plain_text",
                    "text": "Do not Cancel"
                }
              }
            }
          ]
        }
        $bodyBlocks
      ]
		}"""
  }
}
