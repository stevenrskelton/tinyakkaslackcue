package ca.stevenskelton

import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.model.Message
import play.api.libs.functional.syntax._
import play.api.libs.json._

package object tinyakkaslackcue {

  case class SlackTs private(value: String) extends AnyVal

  object SlackTs {
    val Empty = SlackTs("")

    def apply(chatPostMessageResponse: ChatPostMessageResponse): SlackTs = SlackTs(chatPostMessageResponse.getTs)

    def apply(message: Message): SlackTs = SlackTs(message.getTs)
  }

  case class SlackUserId(value: String) extends AnyVal

  case class SlackUser(id: SlackUserId, username: String, name: String, teamId: String)

  object SlackUser {
    implicit val rd: Reads[SlackUser] = (
      (__ \ "id").read[String].map(SlackUserId(_)) and
        (__ \ "username").read[String] and
        (__ \ "name").read[String] and
        (__ \ "team_id").read[String]) (SlackUser.apply _)
  }

  case class SlackBlocksAsString(value: String) extends AnyVal

  case class SlackAction(actionId: String, value: String)

  case class SlackTriggerId(value: String) extends AnyVal

  object SlackAction {
    implicit val rd: Reads[SlackAction] = (
      (__ \ "action_id").read[String] and
        (__ \ "value").read[String]) (SlackAction.apply _)
  }

  case class Mrkdwn(value: String) extends AnyVal {
    override def toString: String = value
  }

  object SlackUserId {
    val Empty = SlackUserId("")
  }

}
