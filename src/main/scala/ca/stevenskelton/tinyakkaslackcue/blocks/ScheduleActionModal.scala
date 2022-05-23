package ca.stevenskelton.tinyakkaslackcue.blocks

import ca.stevenskelton.tinyakkaslackcue._
import org.slf4j.event.Level

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object ScheduleActionModal {

  def cancelledModal(scheduledTask: InteractiveJavaUtilTimer[SlackTask]#ScheduledTask): SlackView = {
    val blocks = if (scheduledTask.isRunning) {
      """
    {
			"type": "section",
			"text": {
				"type": "plain_text",
				"text": "Task has already started, attempting to abort.",
				"emoji": true
			}
		}
    """
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
    SlackView("modal", SlackBlocksAsString(blocks))
  }

  def viewModal(scheduledTasks: Seq[InteractiveJavaUtilTimer[SlackTask]#ScheduledTask], index: Int): SlackBlocksAsString = {
    val scheduledTask = scheduledTasks(index)
    val bodyBlocks = if (scheduledTask.isRunning) {
      s""",{
          "type": "section",
          "text": {
            "type": "plain_text",
            "text": "*Started:* ${scheduledTask.executionStart.toString}"
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

    SlackBlocksAsString(
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
            "text": "${scheduledTask.task.name}",
            "emoji": true
          }
        },
        {
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
              "value": "${scheduledTask.uuid.toString}",
              "action_id": "${ActionId.TaskCancel}",
              "confirm": {
                "title": {
                    "type": "plain_text",
                    "text": "Cancel task ${scheduledTask.task.name}"
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
		}""")
  }

  //https://api.slack.com/reference/surfaces/views
  def createModal(slackUser: SlackUser, slackTaskFactory: SlackTaskFactory, zonedDateTimeOpt: Option[ZonedDateTime], privateMetadata: PrivateMetadata): SlackBlocksAsString = {
    //mrkdwn

    val (headerText, submitButtonText) = if (zonedDateTimeOpt.isEmpty) {
      ("Queue this task immediately.", "Queue")
    } else {
      ("Set later date/time to schedule.", "Schedule")
    }

    val dateTimeBlocks = zonedDateTimeOpt.fold("") {
      zonedDateTime =>
        s""",{
			"type": "input",
			"element": {
				"type": "datepicker",
				"initial_date": "${zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}",
				"placeholder": {
					"type": "plain_text",
					"text": "Select a date",
					"emoji": true
				},
				"action_id": "${ActionId.ScheduleDate.value}"
			},
			"label": {
				"type": "plain_text",
				"text": "Start date",
				"emoji": true
			}
		},
		{
			"type": "input",
			"element": {
				"type": "timepicker",
				"initial_time": "${zonedDateTime.format(DateTimeFormatter.ofPattern("hh:mm"))}",
				"placeholder": {
					"type": "plain_text",
					"text": "Start Time",
					"emoji": true
				},
				"action_id": "${ActionId.ScheduleTime.value}"
			},
			"label": {
				"type": "plain_text",
				"text": "Start time",
				"emoji": true
			}
		}"""
    }

    val advancedOptions =
      s""",{
			"type": "divider"
		},
		{
			"type": "input",
			"element": {
				"type": "multi_users_select",
				"placeholder": {
					"type": "plain_text",
					"text": "Users",
					"emoji": true
				},
        "initial_users": ["${slackUser.id.value}"],
				"action_id": "${ActionId.NotifyOnComplete.value}"
			},
			"label": {
				"type": "plain_text",
				"text": "Users to notify on task complete",
				"emoji": true
			}
		},
		{
			"type": "input",
			"element": {
				"type": "multi_users_select",
				"placeholder": {
					"type": "plain_text",
					"text": "Users",
					"emoji": true
				},
        "initial_users": ["${slackUser.id.value}"],
				"action_id": "${ActionId.NotifyOnFailure.value}"
			},
			"label": {
				"type": "plain_text",
				"text": "Users to notify on task failure",
				"emoji": true
			}
		}"""

    SlackBlocksAsString(
      s"""{
  ${privateMetadata.block},
	"title": {
		"type": "plain_text",
		"text": "$AppModalTitle",
		"emoji": true
	},
	"submit": {
		"type": "plain_text",
		"text": "$submitButtonText",
		"emoji": true
	},
	"type": "modal",
  ${CallbackId.Create.block},
	"close": {
		"type": "plain_text",
		"text": "Cancel",
		"emoji": true
	},
	"blocks": [
     {
      "type": "header",
      "text": {
        "type": "plain_text",
        "text": "New ${slackTaskFactory.name}",
        "emoji": true
      }
    },
		{
			"type": "section",
			"text": {
				"type": "plain_text",
				"text": "$headerText"
			}
		}
		$dateTimeBlocks
    $advancedOptions
		,{
			"type": "divider"
		},
		{
			"type": "input",
			"element": {
				"type": "static_select",
				"placeholder": {
					"type": "plain_text",
					"text": "Select an item",
					"emoji": true
				},
				"options": [${Seq(Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG).map(logLevelBlock).mkString(",")}],
        "initial_option": ${logLevelBlock(Level.WARN)},
				"action_id": "${ActionId.LogLevel.value}"
			},
			"label": {
				"type": "plain_text",
				"text": "Log Level",
				"emoji": true
			}
    }
 	]
}""")
  }

  private def logLevelBlock(level: Level): String =
    s"""{
    "text": {
      "type": "plain_text",
      "text": "${logLevelEmoji(level)} ${level.name}",
      "emoji": true
    },
    "value": "${level.name}"
  }"""

}
