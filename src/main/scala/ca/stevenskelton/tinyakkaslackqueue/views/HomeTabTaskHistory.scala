package ca.stevenskelton.tinyakkaslackqueue.views

import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.TaskHistory
import play.api.libs.json.{JsObject, Json}

import java.time.ZoneId

object HomeTabTaskHistory {
  val BackToFooterBlocks: Seq[JsObject] = Seq(
    Json.obj(
      "type" -> "divider"
    ),
    Json.obj(
      "type" -> "actions",
      "elements" -> Seq(Json.obj(
        "type" -> "button",
        "text" -> Json.obj("type" -> "plain_text", "text" -> ":back: All Tasks", "emoji" -> true),
        "style" -> "primary",
        "action_id" -> ActionId.HomeTabRefresh.value
      ))
    )
  )
}

class HomeTabTaskHistory(zoneId: ZoneId, taskHistory: TaskHistory) extends SlackHomeTab {

  override def toString: String = Json.stringify(blocks)

  def blocks: JsObject = {
    val blocks1 = taskHistory.executed.toSeq.reverse.flatMap {
      obj =>
        val outcomeBlocks = TaskHistory.taskHistoryOutcomeBlocks(obj, zoneId)
        if (outcomeBlocks.isEmpty) Nil
        else {
          outcomeBlocks :+ Json.obj("type" -> "divider")
        }
    }
    val list = if (blocks1.isEmpty) {
      Seq(Json.obj(
        "type" -> "header",
        "text" -> Json.obj(
          "type" -> "plain_text",
          "text" -> "No History",
          "emoji" -> true
        )
      ))
    } else {
      blocks1
    }

    Json.obj(
      "type" -> "home",
      "blocks" -> {
        Seq(
          Json.obj(
            "type" -> "header",
            "text" -> Json.obj(
              "type" -> "plain_text",
              "text" -> taskHistory.slackTaskMeta.factory.name.getText,
              "emoji" -> true
            )
          ),
          Json.obj(
            "type" -> "section",
            "text" -> Json.obj(
              "type" -> "mrkdwn",
              "text" -> taskHistory.slackTaskMeta.factory.description.getText
            )
          ),
          Json.obj(
            "type" -> "divider"
          )
        ) ++ list ++ HomeTabTaskHistory.BackToFooterBlocks
      }
    )
  }
}
