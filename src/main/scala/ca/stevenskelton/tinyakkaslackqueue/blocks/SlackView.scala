package ca.stevenskelton.tinyakkaslackqueue.blocks

import ca.stevenskelton.tinyakkaslackqueue.SlackBlocksAsString
import com.slack.api.model.ModelConfigurator
import com.slack.api.model.block.ActionsBlock.ActionsBlockBuilder
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.element.BlockElements
import com.slack.api.model.view.View

trait SlackView {
  def name: String
}

case class HomeView(blocks: SlackBlocksAsString) extends SlackView {
  override val name = "home"
  override def toString: String = s"""{"type":"$name","blocks":[${blocks.value}]}"""
}

//case class ModalView(blocksAsString: SlackBlocksAsString) extends SlackView {
//  override def toString:String = s"""
//    {
//      "type": "modal",
//      "close": {
//        "type": "plain_text",
//        "text": "Close",
//        "emoji": true
//      },
//      "clear_on_close": true,
//      "title": {
//        "type": "plain_text",
//        "text": "${scheduledTask.task.name}",
//        "emoji": true
//      },
//      "blocks": [
//        $blocks
//      ]
//    }"""
//}

object SlackView {
  def createHomeTab(taskHistories: Seq[TaskHistory]): HomeView = {
    if (taskHistories.isEmpty) {
      val header =
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
      HomeView(SlackBlocksAsString(header))
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
      val blocks = taskHistories.map(_.toBlocks.value).mkString(""",{"type": "divider"},""")
      HomeView(SlackBlocksAsString(header + blocks))
    }
  }
}