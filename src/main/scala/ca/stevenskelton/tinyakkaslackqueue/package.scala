package ca.stevenskelton

import ca.stevenskelton.tinyakkaslackqueue.blocks.ActionId
import ca.stevenskelton.tinyakkaslackqueue.timer.InteractiveJavaUtilTimer
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.model.{Conversation, Message}
import play.api.libs.functional.syntax._
import play.api.libs.json._

package object tinyakkaslackqueue {

  val AppModalTitle = "Tiny Akka Slack Cue"

  type ScheduledSlackTask = InteractiveJavaUtilTimer[SlackTs, SlackTask]#ScheduledTask

  case class SlackTs(value: String) extends AnyVal

  trait SlackMessage {
    def ts: SlackTs

    def channel: SlackChannel

    def url: String = {
      s"https://www.slack.com/archives/${channel.id}/p${ts.value.replace(".", "")}?thread_ts=${ts.value}&cid=${channel.id}"
    }
  }

  trait SlackThread extends SlackMessage

  case class SlackHistoryThread(ts: SlackTs, channel: TaskHistoryChannel) extends SlackThread

  object SlackHistoryThread {
    def apply(message: Message, channel: TaskHistoryChannel): SlackHistoryThread = SlackHistoryThread(SlackTs(message.getTs), channel)
  }

  case class SlackTaskThread(ts: SlackTs, channel: TaskLogChannel) extends SlackThread

  object SlackTaskThread {
    def apply(chatPostMessageResponse: ChatPostMessageResponse, taskLogChannel: TaskLogChannel): SlackTaskThread = {
      SlackTaskThread(SlackTs(chatPostMessageResponse.getTs), taskLogChannel)
    }
  }

  trait SlackChannel {
    def id: String
  }

  case class TaskHistoryChannel(id: String) extends SlackChannel

  case class TaskLogChannel(name: String, id: String) extends SlackChannel

  object SlackChannel {
    def taskId(value: String): SlackChannel = new SlackChannel {
      override def id: String = value
    }
//    def apply(conversation: Conversation): SlackChannel = SlackChannel(conversation.getId)

//    implicit val reads = implicitly[Reads[String]].map(SlackChannel(_))
//    implicit val writes = new Writes[SlackChannel] {
//      override def writes(o: SlackChannel): JsValue = JsString(o.value)
//    }
  }

  case class SlackUserId(value: String) extends AnyVal

  object SlackUserId {
    implicit val reads = implicitly[Reads[String]].map(SlackUserId(_))
    implicit val writes = new Writes[SlackUserId] {
      override def writes(o: SlackUserId): JsValue = JsString(o.value)
    }
  }

  case class SlackUser(id: SlackUserId, username: String, name: String, teamId: String)

  object SlackUser {
    implicit val rd: Reads[SlackUser] = (
      (__ \ "id").read[String].map(SlackUserId(_)) and
        (__ \ "username").read[String] and
        (__ \ "name").read[String] and
        (__ \ "team_id").read[String]) (SlackUser.apply _)
  }

  case class SlackBlocksAsString(value: String) extends AnyVal

  case class SlackAction(actionId: ActionId, value: String)

  case class SlackTriggerId(value: String) extends AnyVal

  object SlackAction {
    implicit val rd: Reads[SlackAction] = (
      (__ \ "action_id").read[String].map(ActionId(_)) and
        (__ \ "value").readNullable[String].map(_.getOrElse(""))) (SlackAction.apply _)
  }

}
