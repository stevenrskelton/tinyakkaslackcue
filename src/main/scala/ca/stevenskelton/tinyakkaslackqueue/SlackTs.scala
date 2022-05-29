package ca.stevenskelton.tinyakkaslackqueue

import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.model.Message

trait SlackTs {
  def value: String

  override def toString: String = value
}

trait SlackThreadTs extends SlackTs

case class SlackHistoryThreadTs(value: String) extends SlackThreadTs

object SlackHistoryThreadTs {
  def apply(message: Message): SlackHistoryThreadTs = SlackHistoryThreadTs(message.getTs)
}

case class SlackTaskThreadTs(value: String) extends SlackThreadTs

object SlackTaskThreadTs {
  def apply(chatPostMessageResponse: ChatPostMessageResponse): SlackTaskThreadTs = SlackTaskThreadTs(chatPostMessageResponse.getTs)
}
