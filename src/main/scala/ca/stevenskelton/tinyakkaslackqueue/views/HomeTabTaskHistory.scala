package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.TaskHistory

object HomeTabTaskHistory {
  val BackToFooterBlocks =
    """
    ,{
		  "type": "divider"
		},{
      "type": "actions",
      "elements": [
        {
          "type": "button",
          "text": {
            "type": "plain_text",
            "text": ":back: All Tasks",
            "emoji": true
          },
          "style": "primary",
          "action_id": "${ActionId.TabRefresh}"
        }
      ]
    }"""
}

class HomeTabTaskHistory(taskHistory: TaskHistory) extends SlackHomeTab {

  override def toString: String = {
    val blocks = taskHistory.executionHistoryBlocks
    val list = if (blocks.isEmpty) {
      """
    ,{
			"type": "header",
			"text": {
				"type": "plain_text",
				"text": "No History",
				"emoji": true
		  }
		}"""
    } else {
      blocks.mkString(""",{"type":"divider"},""")
    }

    s"""
{
  "type":"home",
  "blocks":[
    {
      "type": "header",
      "text": {
        "type": "plain_text",
        "text": "${taskHistory.slackTaskMeta.factory.name.getText}",
        "emoji": true
      }
    },{
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "${taskHistory.slackTaskMeta.factory.description.getText}"
      }
    },{
			"type": "divider"
		},
    $list
    ${HomeTabTaskHistory.BackToFooterBlocks}
  ]
}
"""
  }
}
