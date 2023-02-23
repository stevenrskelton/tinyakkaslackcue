package ca.stevenskelton

import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackTaskFactory}
import ca.stevenskelton.tinyakkaslackqueue.blocks.{ActionId, State}
import ca.stevenskelton.tinyakkaslackqueue.timer.InteractiveJavaUtilTimer
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.model.Message
import play.api.libs.functional.syntax._
import play.api.libs.json._

package object tinyakkaslackqueue {

  val AppModalTitle = "Tiny Akka Slack Cue"

  type ScheduledSlackTask = InteractiveJavaUtilTimer[SlackTs, SlackTask]#ScheduledTask

  case class SlackTs(value: String) extends AnyVal

  object SlackTs {
    val Empty = SlackTs("")
  }

  trait SlackMessage {
    def ts: SlackTs

    def channel: SlackChannel

    def url: String = {
      s"https://www.slack.com/archives/${channel.id}/p${ts.value.replace(".", "")}?thread_ts=${ts.value}&cid=${channel.id}"
    }
  }

  trait SlackThread extends SlackMessage

  case class SlackQueueThread(ts: SlackTs, channel: BotChannel) extends SlackThread

  object SlackQueueThread {
    def apply(message: Message, channel: BotChannel): SlackQueueThread = SlackQueueThread(SlackTs(message.getTs), channel)

    def create(slackTaskFactory: SlackTaskFactory[_, _])(implicit slackClient: SlackClient): Either[SlackQueueThread, String] = {
      val message = s"History: ${slackTaskFactory.name.getText}"
      slackClient.slackConfig.clientOption.map {
        client =>
          val post = client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackClient.slackConfig.botOAuthToken).channel(slackClient.slackConfig.botChannel.id).text(message))
          if (post.isOk) Left(SlackQueueThread(SlackTs(post.getTs), slackClient.slackConfig.botChannel)) else Right(post.getError)
      }.getOrElse {
        Right("Could not initialize Slack client")
      }

    }
  }

  case class SlackTaskThread(ts: SlackTs, channel: TaskLogChannel) extends SlackThread

  object SlackTaskThread {
    def apply(chatPostMessageResponse: ChatPostMessageResponse, taskLogChannel: TaskLogChannel): SlackTaskThread = {
      val threadTs = Option(chatPostMessageResponse.getMessage.getThreadTs).getOrElse(chatPostMessageResponse.getTs)
      SlackTaskThread(SlackTs(threadTs), taskLogChannel)
    }
  }

  trait SlackChannel {
    def id: String
  }

  case class BotChannel(id: String) extends SlackChannel

  case class TaskLogChannel(id: String) extends SlackChannel

  object SlackChannel {
    def taskId(value: String): SlackChannel = new SlackChannel {
      override def id: String = value
    }
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
    val System = SlackUser(SlackUserId(""), "System", "System", "")

    implicit val rd: Reads[SlackUser] = (
      (__ \ "id").read[String].map(SlackUserId(_)) and
        (__ \ "username").read[String] and
        (__ \ "name").read[String] and
        (__ \ "team_id").read[String])(SlackUser.apply _)
  }

  case class SlackBlocksAsString(value: String) extends AnyVal

  case class SlackAction(actionId: ActionId, state: State)

  case class SlackTriggerId(value: String) extends AnyVal

  object SlackTriggerId {
    val Empty = SlackTriggerId("")
  }

  object SlackAction {
    implicit val rd: Reads[SlackAction] = ((__ \ "action_id").read[String].map(ActionId(_)) and __.read[State])(SlackAction.apply _)
  }

}
