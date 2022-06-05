package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.api.SlackFactories
import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId

import java.time.ZoneId

class HomeTabConfigure(zoneId: ZoneId)(implicit slackFactories: SlackFactories) extends SlackHomeTab {

  override def toString: String = {

    val logChannels = slackFactories.factoryLogChannels
    val channels = if (logChannels.isEmpty) {
      """{
			"type": "header",
			"text": {
				"type": "plain_text",
				"text": "No Tasks Found. Override `SlackFactories.factories`",
				"emoji": true
			}
		}"""
    } else {
      logChannels.zipWithIndex.map {
        case ((slackTaskFactory, taskLogChannelOpt), index) =>
          s"""
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "*${slackTaskFactory.name.getText}*\n${slackTaskFactory.description}"
  },
  "accessory": {
    "type": "channels_select",
    "placeholder": {
      "type": "plain_text",
      "text": "Select a channel",
      "emoji": true
    },
    ${taskLogChannelOpt.fold("")(channel => s""""initial_channel":"${channel.id}",""")}
    "action_id": "${ActionId.DataChannel}-$index"
  }
}
          """
      }.mkString(",")
    }

    s"""
{
  "type":"home",
  "blocks":[
    {
      "type": "header",
      "text": {
        "type": "plain_text",
        "text": ":card_index: Tiny Akka Slack Cue Settings",
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
    },
    {
			"type": "divider"
		},
    $channels,
    ${HomeTabTaskHistory.BackToFooterBlocks}
  ]
}
"""
  }
}
