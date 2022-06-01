package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.SlackTaskMeta
import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId

class HomeTabTaskHistory(slackTaskMeta: SlackTaskMeta) extends SlackHomeTab {

  override def toString: String = {
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
    {
      "type": "header",
      "text": {
        "type": "plain_text",
        "text": "${slackTaskMeta.factory.name.getText}",
        "emoji": true
      }
    },{
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "${slackTaskMeta.factory.description.getText}"
      }
    },{
      "type": "actions",
      "elements": [
      ]
    }
  ]
}
"""
  }
}
