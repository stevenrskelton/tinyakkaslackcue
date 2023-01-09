package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.SlackFactories
import ca.stevenskelton.tinyakkaslackqueue.blocks.{ActionId, CallbackId}
import play.api.libs.json.{JsObject, Json}

import java.time.ZoneId

object HomeTabConfigure {

  private val NoTasksBlocks = Seq(Json.obj(
    "type" -> "header",
    "text" -> Json.obj(
      "type" -> "plain_text",
      "text" -> "No Tasks Found. Override `SlackFactories.factories`",
      "emoji" -> true
    )
  ))

  private val HeaderBlocks = Seq(
    Json.obj(
      "type" -> "header",
      "text" -> Json.obj(
        "type" -> "plain_text",
        "text" -> ":card_index: Tiny Akka Slack Cue Settings",
        "emoji" -> true
      )
    ),
    Json.obj(
      "type" -> "section",
      "fields" -> Seq(Json.obj(
        "type" -> "mrkdwn",
        "text" -> "Warning: Changing task channels will reset any previous history for that task."
      ))
    ),
    Json.obj("type" -> "divider")
  )

}

class HomeTabConfigure(zoneId: ZoneId)(implicit slackFactories: SlackFactories) extends SlackHomeTab {

  override def toString: String = Json.stringify(blocks)

  def blocks: JsObject = {
    val channels: Seq[JsObject] = if (slackFactories.factoryLogChannels.isEmpty) {
      HomeTabConfigure.NoTasksBlocks
    } else {
      val allChannels = slackFactories.slackClient.allChannels

      slackFactories.factoryLogChannels.zipWithIndex.map {
        case ((slackTaskFactory, taskLogChannelOpt), index) =>

          val exists = allChannels.exists(o => taskLogChannelOpt.exists(_.id == o.getId))
          val text = if (taskLogChannelOpt.isEmpty) {
            "Select a channel"
          } else if (exists) {
            "Change channel"
          } else {
            ":red_circle: Missing channel"
          }

          val accessory = Json.obj(
            "type" -> "channels_select",
            "placeholder" -> Json.obj(
              "type" -> "plain_text",
              "text" -> text,
              "emoji" -> true
            ),
            "action_id" -> s"${ActionId.DataChannel}-$index"
          )
          val accessory2 = taskLogChannelOpt.map {
            channel => accessory ++ Json.obj("initial_channel" -> channel.id)
          }.getOrElse {
            accessory
          }

          Json.obj(
            "type" -> "section",
            "text" -> Json.obj(
              "type" -> "mrkdwn",
              "text" -> s"*${slackTaskFactory.name.getText}*\n${slackTaskFactory.description.getText}"
            ),
            "accessory" -> accessory2
          )
      }
    }
    Json.obj(
      "type" -> "home",
      CallbackId.HomeTabConfigure.block,
      "blocks" -> { HomeTabConfigure.HeaderBlocks ++ channels :+ HomeTabTaskHistory.BackToFooterBlocks }
    )
  }
}
