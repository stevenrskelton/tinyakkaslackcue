package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId
import ca.stevenskelton.tinyakkaslackqueue.{ScheduledSlackTask, SlackFactories, SlackThread}

import java.time.ZoneId

object HomeTab {

  def viewLogsButton(slackThread: SlackThread): String = {
    s"""
       {
				"type": "button",
				"text": {
					"type": "plain_text",
					"text": "View Logs",
					"emoji": true
				},
        "url": "${slackThread.url}",
        "action_id": "${ActionId.RedirectToTaskThread}",
			}
       """
  }

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

class HomeTab(zoneId: ZoneId)(implicit slackFactories: SlackFactories) extends SlackHomeTab {

  //TODO: sort taskHistories
  private val taskHistories = slackFactories.history

  override def toString: String = {
    if (taskHistories.isEmpty || taskHistories.size != slackFactories.slackTasks.size) new HomeTabConfigure(zoneId).toString else
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
            "text": ":arrows_counterclockwise: Refresh Statuses",
            "emoji": true
          },
          "style": "primary",
          "action_id": "${ActionId.HomeTabRefresh}"
        },{
          "type": "button",
          "text": {
            "type": "plain_text",
            "text": ":wrench: Configure",
            "emoji": true
          },
          "action_id": "${ActionId.HomeTabConfiguration}"
        }
      ]
    },{
      "type": "divider"
    },
    ${taskHistories.map(_.homeTabBlocks(zoneId)).mkString(""",{"type": "divider"},""")}
  ]
}
"""
  }
}