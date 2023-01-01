package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.SlackFactories
import ca.stevenskelton.tinyakkaslackqueue.blocks.{ActionId, CallbackId}
import play.api.libs.json.{JsObject, Json}

import java.time.ZoneId

class HomeTabConfigure(zoneId: ZoneId)(implicit slackFactories: SlackFactories) extends SlackHomeTab {

  def blocks: JsObject = {

    val logChannels = slackFactories.factoryLogChannels
    val channels: Seq[JsObject] = if (logChannels.isEmpty) {
      Seq(Json.obj(
        "type" -> "header",
        "text" -> Json.obj(
          "type" -> "plain_text",
          "text" -> "No Tasks Found. Override `SlackFactories.factories`",
          "emoji" -> true
        )
      ))
    } else {
      logChannels.zipWithIndex.map {
        case ((slackTaskFactory, taskLogChannelOpt), index) =>
          val accessory = Json.obj(
            "type" -> "channels_select",
            "placeholder" -> Json.obj(
              "type" -> "plain_text",
              "text" -> "Select a channel",
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
    val blocks = Seq(
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
    ) ++ channels ++ HomeTabTaskHistory.BackToFooterBlocks

    Json.obj(
      "type" -> "home",
      CallbackId.HomeTabConfigure.block,
      "blocks" -> blocks
    )
  }
}
