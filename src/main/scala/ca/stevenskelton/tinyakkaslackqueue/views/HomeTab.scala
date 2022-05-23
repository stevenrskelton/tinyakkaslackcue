package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.blocks.{ActionId, TaskHistory}

class HomeTab(taskHistories: Seq[TaskHistory]) extends SlackView {
  private val blocks = if (taskHistories.isEmpty) {
    s"""
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
      }"""
  } else {
    val header =
      s"""
      {
        "type": "actions",
        "elements": [
          {
            "type": "button",
            "text": {
              "type": "plain_text",
              "text": "Refresh :card_index: Statuses",
              "emoji": true
            },
            "style": "primary",
            "action_id": "${ActionId.TabRefresh}"
          }
        ]
      },{"type": "divider"},"""
    header + taskHistories.map(_.toBlocks.value).mkString(""",{"type": "divider"},""")
  }
  override def toString: String = s"""{"type":"home","blocks":[$blocks]}"""
}