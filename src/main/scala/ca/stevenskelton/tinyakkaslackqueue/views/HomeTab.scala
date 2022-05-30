package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.ScheduledSlackTask
import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.TaskHistory

object HomeTab {

  def viewLogsButton(scheduledTask: ScheduledSlackTask) =
    s"""
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "View Logs"
      },
      "action_id": "${ActionId.TaskThread}",
      "value": "${scheduledTask.task.id.value}"
    }"""

  def cancelTaskButton(scheduledTask: ScheduledSlackTask, actionId: ActionId) =
    s"""
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "text": "Cancel Task",
        "emoji": true
      },
      "style": "danger",
      "value": "${scheduledTask.task.id.value}",
      "action_id": "$actionId",
      "confirm": {
        "title": {
            "type": "plain_text",
            "text": "Cancel task ${scheduledTask.task.meta.factory.name.getText}"
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
    }"""

}

class HomeTab(taskHistories: Iterable[TaskHistory]) extends SlackView {

  override def toString: String = {
    if (taskHistories.isEmpty)
      s"""
{
  "type":"home",
  "blocks":[
    {
      "type": "header",
      "text": {
        "type": "plain_text",
        "text": ":card_index: Tiny Akka Slack Cue",
        "emoji": true
      }
    },{
      "type": "section",
      "fields": [
        {
          "type": "mrkdwn",
          "text": "*Configure Tasks*\nTasks are cancellable and can be queued"
        }
      ]
    }
  ]
}
""" else
      s"""
{
  "type":"home",
  "blocks":[
    {
      "type": "actions",
      "elements": [
        {
          "type": "button",
          "text": {
            "type": "plain_text",
            "text": ":card_index: Refresh Statuses",
            "emoji": true
          },
          "style": "primary",
          "action_id": "${ActionId.TabRefresh}"
        }
      ]
    },{
      "type": "divider"
    },
    ${taskHistories.map(_.homeTabBlocks).mkString(""",{"type": "divider"},""")}
  ]
}
"""
  }
}