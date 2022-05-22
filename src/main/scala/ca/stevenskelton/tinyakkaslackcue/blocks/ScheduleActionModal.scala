package ca.stevenskelton.tinyakkaslackcue.blocks

import ca.stevenskelton.tinyakkaslackcue.{InteractiveJavaUtilTimer, SlackBlocksAsString, SlackTask, SlackTaskFactory, SlackUser}
import org.slf4j.event.Level
import play.api.libs.json.JsObject

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object ScheduleActionModal {

  def viewModal(scheduledTask: InteractiveJavaUtilTimer[SlackTask]#ScheduledTask): SlackBlocksAsString = {

        SlackBlocksAsString(
      s"""{
	"title": {
		"type": "plain_text",
		"text": "${scheduledTask.task.name}",
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
			"type": "actions",
			"elements": [
				{
					"type": "button",
					"text": {
						"type": "plain_text",
						"text": "Cancel",
						"emoji": true
					},
					"style": "danger",
					"value": "${scheduledTask.uuid.toString}",
					"action_id": "${ActionId.TaskCancel}"
				}
			]
		},
		{
			"type": "section",
			"text": {
				"type": "plain_text",
				"text": "*Started:* ${scheduledTask.executionStart.toString}"
			}
		}
	]
		}""")

  }

  //https://api.slack.com/reference/surfaces/views
  def createModal(slackUser: SlackUser, slackTaskFactory: SlackTaskFactory, zonedDateTimeOpt: Option[ZonedDateTime], privateMetadata: PrivateMetadata): SlackBlocksAsString = {
    //mrkdwn

    val (headerText, submitButtonText) = if(zonedDateTimeOpt.isEmpty){
      ("Queue this task immediately.","Queue")
    }else{
      ("Set later date/time to schedule.","Schedule")
    }

    val dateTimeBlocks = zonedDateTimeOpt.fold("") {
      zonedDateTime =>
       s"""{
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
		},"""
    }

    SlackBlocksAsString(
      s"""{
  ${privateMetadata.block},
	"title": {
		"type": "plain_text",
		"text": "New ${slackTaskFactory.name.take(21)}",
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
			"type": "section",
			"text": {
				"type": "plain_text",
				"text": "$headerText"
			}
		},
		$dateTimeBlocks
		{
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
				"action_id": "${ActionId.NotifyOnFailure.value}"
			},
			"label": {
				"type": "plain_text",
				"text": "Users to notify on task failure",
				"emoji": true
			}
		},
		{
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
				"options": [${
        Seq(Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG).map {
          level =>
            s"""{
						"text": {
							"type": "plain_text",
							"text": "${logLevelEmoji(level)} ${level.name}",
							"emoji": true
						},
						"value": "${level.name}"
					}"""
        }.mkString(",")
      }],
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

}
