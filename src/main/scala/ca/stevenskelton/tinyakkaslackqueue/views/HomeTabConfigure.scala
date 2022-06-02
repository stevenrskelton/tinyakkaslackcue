package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.api.SlackFactories

class HomeTabConfigure()(implicit slackFactories: SlackFactories) extends SlackHomeTab {

  override def toString: String = {
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
    }
    ${HomeTabTaskHistory.BackToFooterBlocks}
  ]
}
"""
  }
}
