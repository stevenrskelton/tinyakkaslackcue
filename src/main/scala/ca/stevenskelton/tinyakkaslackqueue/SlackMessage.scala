package ca.stevenskelton.tinyakkaslackqueue

import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.model.Message

case class SlackTs(value: String) extends AnyVal

trait SlackMessage {
  def ts: SlackTs
  def channel: SlackChannel
}

trait SlackThread extends SlackMessage

case class SlackHistoryThread(ts: SlackTs, channel: SlackChannel) extends SlackThread

object SlackHistoryThread {
  def apply(message: Message, channel: SlackChannel): SlackHistoryThread = SlackHistoryThread(SlackTs(message.getTs), channel)
}

case class SlackTaskThread(ts: SlackTs, channel: SlackChannel) extends SlackThread

object SlackTaskThread {
  def apply(chatPostMessageResponse: ChatPostMessageResponse): SlackTaskThread = SlackTaskThread(SlackTs(chatPostMessageResponse.getTs), SlackChannel(chatPostMessageResponse.getChannel))
}
