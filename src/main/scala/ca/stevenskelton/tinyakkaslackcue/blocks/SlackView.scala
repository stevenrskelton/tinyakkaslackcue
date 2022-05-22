package ca.stevenskelton.tinyakkaslackcue.blocks

import ca.stevenskelton.tinyakkaslackcue.{AppHomeTitle, SlackBlocksAsString}

case class SlackView(name: String, blocks: SlackBlocksAsString) {
  override def toString: String = s"""{"type":"$name","blocks":[${blocks.value}]}"""
}

object SlackView {
  def createHomeTab(taskHistories: Seq[TaskHistory]): SlackView = {
    if(taskHistories.isEmpty){
      val header = s"""
      {
        "type": "header",
        "text": {
          "type": "plain_text",
          "text": "$AppHomeTitle",
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
      SlackView("home", SlackBlocksAsString(header))
    }else{
      val header = s"""
      {
        "type": "header",
        "text": {
          "type": "plain_text",
          "text": "$AppHomeTitle",
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
              "text": "Refresh Page",
              "emoji": true
            },
            "style": "primary",
            "action_id": "${ActionId.TabRefresh}"
          }
        ]
      },{"type": "divider"},"""
      val blocks = taskHistories.map(_.toBlocks.value).mkString(""",{"type": "divider"},""")
      SlackView("home", SlackBlocksAsString(header + blocks))
    }
  }
}